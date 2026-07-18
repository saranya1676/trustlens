/**
 * TruthLens AI — chatbot.js
 * Full chatbot UI: toggle, messaging, article cards, quick replies, markdown rendering.
 */

'use strict';

// ── State ─────────────────────────────────────────────────────
const Chat = {
  sessionId:   generateSessionId(),
  isOpen:      false,
  isTyping:    false,
  msgCount:    0,
  unread:      0,
  initialized: false
};

// ── Category badge colours (matches CSS classes in style.css) ──
const CAT_COLORS = {
  INTERNATIONAL: '#0ea5e9',
  NATIONAL:      '#10b981',
  POLITICS:      '#f59e0b',
  SPORTS:        '#ef4444',
  TECHNOLOGY:    '#6366f1'
};

// ── Quick-reply suggestion sets ───────────────────────────────
const QUICK_REPLIES_INIT = [
  'Latest news',
  'Show technology articles',
  'How does verification work?',
  'Sports news',
  'Politics updates'
];

const QUICK_REPLIES_AFTER = [
  'Tell me more',
  'Show latest news',
  'How to verify news?',
  'Browse categories'
];

// =========================================================================
// Initialisation
// =========================================================================

document.addEventListener('DOMContentLoaded', initChatbot);

function initChatbot() {
  const toggle  = document.getElementById('chatbotToggle');
  const window_ = document.getElementById('chatbotWindow');
  const closeBtn= document.getElementById('chatbotClose');
  const input   = document.getElementById('chatbotInput');
  const sendBtn = document.getElementById('chatbotSend');

  if (!toggle || !window_) return; // widget not present on this page

  // Toggle open/close
  toggle.addEventListener('click', toggleChat);
  closeBtn.addEventListener('click', closeChat);

  // Send on button click
  sendBtn.addEventListener('click', sendMessage);

  // Send on Enter (Shift+Enter = newline)
  input.addEventListener('keydown', e => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });

  // Auto-resize textarea
  input.addEventListener('input', () => {
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 100) + 'px';
    document.getElementById('chatbotSend').disabled = !input.value.trim();
  });

  // Close when clicking outside
  document.addEventListener('click', e => {
    if (Chat.isOpen && !window_.contains(e.target) && !toggle.contains(e.target)) {
      closeChat();
    }
  });

  // Show welcome notification badge after 3 s
  setTimeout(() => {
    if (!Chat.isOpen) showBadge(1);
  }, 3000);
}

// =========================================================================
// Open / Close
// =========================================================================

function toggleChat() {
  Chat.isOpen ? closeChat() : openChat();
}

function openChat() {
  const win = document.getElementById('chatbotWindow');
  const ico = document.getElementById('chatbotToggleIcon');
  win.classList.add('open');
  Chat.isOpen = true;
  clearBadge();

  // Change icon to X
  if (ico) { ico.className = 'bi bi-x-lg'; }

  // Send welcome message on first open
  if (!Chat.initialized) {
    Chat.initialized = true;
    setTimeout(() => addWelcomeMessage(), 300);
  }

  // Focus input
  setTimeout(() => {
    const inp = document.getElementById('chatbotInput');
    if (inp) inp.focus();
  }, 350);
}

function closeChat() {
  const win = document.getElementById('chatbotWindow');
  const ico = document.getElementById('chatbotToggleIcon');
  win.classList.remove('open');
  Chat.isOpen = false;
  if (ico) { ico.className = 'bi bi-chat-dots-fill'; }
}

// =========================================================================
// Welcome message
// =========================================================================

function addWelcomeMessage() {
  const text =
    '👋 Hi! I\'m the <strong>TruthLens AI Assistant</strong>.<br><br>' +
    'I\'ve analysed all the news articles and trusted sources in our database. ' +
    'Ask me anything — I\'ll find relevant articles and answer your questions instantly.';

  addBotMessage(text, QUICK_REPLIES_INIT);
}

// =========================================================================
// Send message
// =========================================================================

async function sendMessage() {
  const input   = document.getElementById('chatbotInput');
  const sendBtn = document.getElementById('chatbotSend');
  const message = input.value.trim();
  if (!message || Chat.isTyping) return;

  // Add user bubble
  addUserMessage(message);
  input.value = '';
  input.style.height = 'auto';
  sendBtn.disabled = true;

  // Show typing indicator
  showTyping();
  Chat.isTyping = true;

  try {
    const res  = await fetch('/api/chat', {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({ message, sessionId: Chat.sessionId })
    });
    const data = await res.json();

    hideTyping();
    Chat.isTyping = false;

    if (data.success && data.data) {
      const r = data.data;
      renderBotResponse(r);
    } else {
      addBotMessage('Sorry, I couldn\'t process that. Please try again.', []);
    }
  } catch (err) {
    hideTyping();
    Chat.isTyping = false;
    addBotMessage(
      '⚠️ I\'m having trouble connecting right now. ' +
      'Please make sure the server is running and try again.',
      ['Try again', 'Latest news']
    );
  }
}

// =========================================================================
// Render bot response
// =========================================================================

function renderBotResponse(response) {
  const articles     = response.relatedArticles || [];
  const quickReplies = articles.length ? QUICK_REPLIES_AFTER : QUICK_REPLIES_INIT;

  // Convert markdown-style text to HTML
  const html = markdownToHtml(response.answer || '');

  // Build article cards HTML
  let articlesHtml = '';
  if (articles.length) {
    articlesHtml = `<div class="chat-articles">` +
      articles.map(a => buildArticleCard(a)).join('') +
      `</div>`;
  }

  addBotMessageHtml(html + articlesHtml, quickReplies, response.confidence);
}

// =========================================================================
// DOM helpers
// =========================================================================

function addUserMessage(text) {
  const msgs = document.getElementById('chatbotMessages');
  const div  = document.createElement('div');
  div.className = 'chat-msg user';
  div.innerHTML = `
    <div class="chat-bubble">${escapeHtmlChat(text)}</div>
    <div class="chat-time">${now()}</div>`;
  msgs.appendChild(div);
  scrollToBottom();
  Chat.msgCount++;
}

function addBotMessage(text, quickReplies = []) {
  addBotMessageHtml(markdownToHtml(text), quickReplies, null);
}

function addBotMessageHtml(html, quickReplies = [], confidence = null) {
  const msgs = document.getElementById('chatbotMessages');
  const div  = document.createElement('div');
  div.className = 'chat-msg bot';

  let qrHtml = '';
  if (quickReplies.length) {
    qrHtml = `<div class="chat-quick-replies">` +
      quickReplies.map(q =>
        `<button class="chat-chip" onclick="sendQuickReply('${escapeAttr(q)}')">${escapeHtmlChat(q)}</button>`
      ).join('') +
      `</div>`;
  }

  let confHtml = '';
  if (confidence !== null && confidence > 0) {
    confHtml = `<div class="chat-time">Confidence: ${confidence}%</div>`;
  }

  div.innerHTML = `
    <div class="chat-bubble">${html}</div>
    ${qrHtml}
    <div class="chat-time">${now()}</div>
    ${confHtml}`;

  msgs.appendChild(div);
  scrollToBottom();
  Chat.msgCount++;

  // Show badge if window is closed
  if (!Chat.isOpen) {
    Chat.unread++;
    showBadge(Chat.unread);
  }
}

function buildArticleCard(article) {
  const color  = CAT_COLORS[article.category] || '#64748b';
  const catName = article.categoryDisplayName || article.category || '';
  const title   = escapeHtmlChat(article.title || 'Article');
  const source  = escapeHtmlChat(article.sourceName || '');
  const snippet = escapeHtmlChat(article.snippet || '');
  const score   = article.relevanceScore || 0;

  return `
  <div class="chat-article-card" onclick="openArticleFromChat(${article.id})">
    <div class="chat-article-title">${title}</div>
    <div class="chat-article-meta">
      <span class="chat-article-badge" style="background:${color}">${catName}</span>
      ${source ? `<span>${source}</span>` : ''}
      ${score > 0 ? `<span style="margin-left:auto;">${score}% match</span>` : ''}
    </div>
    ${snippet ? `<div style="font-size:.72rem;color:#64748b;margin-top:.25rem;
      display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
      ${snippet}</div>` : ''}
  </div>`;
}

// =========================================================================
// Typing indicator
// =========================================================================

function showTyping() {
  const msgs = document.getElementById('chatbotMessages');
  const div  = document.createElement('div');
  div.className = 'chat-msg bot chat-typing';
  div.id = 'chatTypingIndicator';
  div.innerHTML = `<div class="chat-bubble">
    <span class="typing-dot"></span>
    <span class="typing-dot"></span>
    <span class="typing-dot"></span>
  </div>`;
  msgs.appendChild(div);
  scrollToBottom();
}

function hideTyping() {
  const el = document.getElementById('chatTypingIndicator');
  if (el) el.remove();
}

// =========================================================================
// Quick replies
// =========================================================================

function sendQuickReply(text) {
  const input = document.getElementById('chatbotInput');
  if (input) {
    input.value = text;
    input.dispatchEvent(new Event('input'));
  }
  sendMessage();
}

// =========================================================================
// Article modal integration
// =========================================================================

function openArticleFromChat(articleId) {
  if (typeof showArticleModal === 'function') {
    showArticleModal(articleId);
  } else {
    window.location.href = `/news?id=${articleId}`;
  }
}

// =========================================================================
// Badge helpers
// =========================================================================

function showBadge(count) {
  let badge = document.getElementById('chatbotBadge');
  if (!badge) {
    badge = document.createElement('span');
    badge.id = 'chatbotBadge';
    badge.className = 'chatbot-badge';
    document.getElementById('chatbotToggle').appendChild(badge);
  }
  badge.textContent = count > 9 ? '9+' : count;
  badge.style.display = 'flex';
}

function clearBadge() {
  Chat.unread = 0;
  const badge = document.getElementById('chatbotBadge');
  if (badge) badge.style.display = 'none';
}

// =========================================================================
// Utilities
// =========================================================================

function scrollToBottom() {
  const msgs = document.getElementById('chatbotMessages');
  if (msgs) setTimeout(() => { msgs.scrollTop = msgs.scrollHeight; }, 50);
}

function now() {
  return new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
}

function generateSessionId() {
  return 'sess_' + Math.random().toString(36).slice(2, 11) + Date.now().toString(36);
}

function escapeHtmlChat(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function escapeAttr(str) {
  return String(str || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

/**
 * Converts a small subset of Markdown to HTML:
 * **bold**, *italic*, [link](url), line breaks, bullet lists.
 */
function markdownToHtml(text) {
  if (!text) return '';
  return text
    // Bold
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // Italic
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // Links  [text](url)
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
    // Bullet list lines starting with •
    .replace(/^• (.+)$/gm, '<li>$1</li>')
    // Wrap consecutive <li> in <ul>
    .replace(/(<li>.*<\/li>)/gs, '<ul style="margin:.3rem 0 .3rem 1rem;padding:0;">$1</ul>')
    // Line breaks
    .replace(/\n\n/g, '<br><br>')
    .replace(/\n/g, '<br>');
}
