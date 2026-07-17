/**
 * TruthLens AI — main.js
 * Shared utilities, news loading, card rendering, and navbar search.
 * Used across all pages.
 */

'use strict';

/* ================================================================
   NAVBAR SEARCH
   ================================================================ */
function handleNavSearch(e) {
    e.preventDefault();
    const q = document.getElementById('navSearchInput').value.trim();
    if (q) window.location.href = `/news?q=${encodeURIComponent(q)}`;
}

/* ================================================================
   VERIFICATION STATS  (homepage hero counters)
   ================================================================ */
async function loadVerificationStats() {
    try {
        const res  = await fetch('/api/verify/stats');
        const data = await res.json();
        if (!data.success) return;
        const d = data.data;
        animateCounter('statVerified',   d.likelyTrue        || 0);
        animateCounter('statFalse',      d.likelyFalse       || 0);
        animateCounter('statUnverified', d.needsVerification || 0);
    } catch (_) { /* silently ignore if page element absent */ }
}

async function loadNewsStats() {
    try {
        const res  = await fetch('/api/news/stats');
        const data = await res.json();
        if (!data.success) return;
        animateCounter('statArticles', data.data.total || 0);
    } catch (_) {}
}

/** Animate a number from 0 to target over ~800ms */
function animateCounter(elementId, target) {
    const el = document.getElementById(elementId);
    if (!el) return;
    const duration = 800;
    const steps    = 40;
    const increment = target / steps;
    let current = 0;
    const timer = setInterval(() => {
        current = Math.min(current + increment, target);
        el.textContent = Math.round(current).toLocaleString();
        if (current >= target) clearInterval(timer);
    }, duration / steps);
}

/* ================================================================
   FEATURED ARTICLES
   ================================================================ */
async function loadFeaturedArticles() {
    const container = document.getElementById('featuredArticlesContainer');
    if (!container) return;

    try {
        const res  = await fetch('/api/news/featured');
        const data = await res.json();
        if (!data.success || !data.data.length) {
            container.innerHTML = renderEmptyState('No featured articles available.');
            return;
        }
        const articles = data.data;
        const [hero, ...rest] = articles;
        let html = `<div class="col-lg-7">${createArticleCard(hero, true)}</div>`;
        if (rest.length) {
            html += `<div class="col-lg-5"><div class="row gy-3">
                ${rest.slice(0, 4).map(a => `<div class="col-12">${createArticleCardSmall(a)}</div>`).join('')}
            </div></div>`;
        }
        container.innerHTML = html;
    } catch (err) {
        container.innerHTML = `<div class="col-12"><div class="alert alert-warning">
            <i class="bi bi-exclamation-triangle me-2"></i>Could not load featured articles.</div></div>`;
    }
}

/* ================================================================
   CATEGORY ARTICLES  (homepage sections)
   ================================================================ */
async function loadCategoryArticles(category, containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    try {
        const res  = await fetch(`/api/news/category/${category}/latest`);
        const data = await res.json();
        if (!data.success || !data.data.length) {
            container.innerHTML = `<div class="col-12">${renderEmptyState('No articles in this category yet.')}</div>`;
            return;
        }
        container.innerHTML = data.data
            .map(a => `<div class="col-md-4 col-sm-6">${createArticleCard(a)}</div>`)
            .join('');
    } catch (_) {
        container.innerHTML = `<div class="col-12 text-muted small text-center py-3">
            <i class="bi bi-wifi-off me-1"></i>Could not load articles.</div>`;
    }
}

/* ================================================================
   ARTICLE CARD BUILDERS
   ================================================================ */

/** Full-size article card (used in grids) */
function createArticleCard(article, featured = false) {
    const imgHtml = article.imageUrl
        ? `<img src="${escapeHtml(article.imageUrl)}" alt="${escapeHtml(article.title)}"
               loading="lazy" onerror="this.parentElement.innerHTML=renderImgPlaceholder('${article.category}');">`
        : renderImgPlaceholder(article.category);

    const date = article.publishedDate
        ? formatDate(article.publishedDate) : '';

    return `
    <div class="news-card ${featured ? 'news-card-featured' : ''} h-100">
        <div class="news-card-img-wrapper">${imgHtml}</div>
        <div class="news-card-body">
            <div class="news-card-category">
                <span class="badge ${article.categoryBadgeClass || 'bg-secondary'} text-white">
                    ${escapeHtml(article.categoryDisplayName || article.category)}
                </span>
            </div>
            <h5 class="news-card-title">
                <a href="#" onclick="showArticleModal(${article.id}); return false;">
                    ${escapeHtml(article.title)}
                </a>
            </h5>
            <p class="news-card-summary">${escapeHtml(article.shortSummary || article.summary || '')}</p>
            <div class="news-card-footer">
                <span><i class="bi bi-person me-1"></i>${escapeHtml(article.author || 'Staff Reporter')}</span>
                <span class="d-flex align-items-center gap-2">
                    ${date ? `<span><i class="bi bi-calendar3 me-1"></i>${date}</span>` : ''}
                    <span><i class="bi bi-eye me-1"></i>${(article.viewCount || 0).toLocaleString()}</span>
                </span>
            </div>
        </div>
    </div>`;
}

/** Compact horizontal card (sidebar / small slots) */
function createArticleCardSmall(article) {
    const imgHtml = article.imageUrl
        ? `<img src="${escapeHtml(article.imageUrl)}" alt="${escapeHtml(article.title)}"
               style="width:80px;height:70px;object-fit:cover;border-radius:8px;flex-shrink:0;"
               loading="lazy">`
        : `<div style="width:80px;height:70px;border-radius:8px;flex-shrink:0;background:#e2e8f0;
               display:flex;align-items:center;justify-content:center;font-size:1.2rem;color:#94a3b8;">
               <i class="bi ${getCategoryIcon(article.category)}"></i></div>`;

    return `
    <div class="d-flex gap-3 align-items-start p-2 rounded hover-bg"
         style="cursor:pointer;transition:background .2s;"
         onclick="showArticleModal(${article.id})"
         onmouseenter="this.style.background='#f8fafc'"
         onmouseleave="this.style.background='transparent'">
        ${imgHtml}
        <div class="flex-grow-1 min-w-0">
            <span class="badge ${article.categoryBadgeClass || 'bg-secondary'} text-white mb-1"
                  style="font-size:.68rem;">
                ${escapeHtml(article.categoryDisplayName || article.category)}
            </span>
            <p class="fw-semibold mb-0 small" style="line-height:1.35;display:-webkit-box;
               -webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
                ${escapeHtml(article.title)}
            </p>
            <small class="text-muted">${formatDate(article.publishedDate)}</small>
        </div>
    </div>`;
}

function renderImgPlaceholder(category) {
    return `<div class="news-card-img-placeholder">
        <i class="bi ${getCategoryIcon(category)}"></i></div>`;
}

function renderEmptyState(msg) {
    return `<div class="text-center py-4 text-muted">
        <i class="bi bi-newspaper display-6"></i>
        <p class="mt-2 mb-0 small">${msg}</p></div>`;
}

/* ================================================================
   PAGINATION RENDERER
   ================================================================ */
function renderPagination(currentPage, totalPages) {
    const container = document.getElementById('paginationContainer');
    const nav       = document.getElementById('pagination');
    if (!container || !nav || totalPages <= 1) {
        if (container) container.classList.add('d-none');
        return;
    }

    container.classList.remove('d-none');
    const pages = [];

    // Prev
    pages.push(`<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
        <button class="page-link" onclick="goToPage(${currentPage - 1})" ${currentPage === 0 ? 'disabled' : ''}>
            <i class="bi bi-chevron-left"></i></button></li>`);

    // Page numbers with ellipsis
    const range = buildPageRange(currentPage, totalPages);
    range.forEach(p => {
        if (p === '...') {
            pages.push(`<li class="page-item disabled"><span class="page-link">…</span></li>`);
        } else {
            pages.push(`<li class="page-item ${p === currentPage ? 'active' : ''}">
                <button class="page-link" onclick="goToPage(${p})">${p + 1}</button></li>`);
        }
    });

    // Next
    pages.push(`<li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
        <button class="page-link" onclick="goToPage(${currentPage + 1})"
                ${currentPage >= totalPages - 1 ? 'disabled' : ''}>
            <i class="bi bi-chevron-right"></i></button></li>`);

    nav.innerHTML = pages.join('');
}

function buildPageRange(current, total) {
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    if (current < 4) return [0, 1, 2, 3, 4, '...', total - 1];
    if (current > total - 5) return [0, '...', total - 5, total - 4, total - 3, total - 2, total - 1];
    return [0, '...', current - 1, current, current + 1, '...', total - 1];
}

/* ================================================================
   ARTICLE DETAIL MODAL
   ================================================================ */
async function showArticleModal(articleId) {
    // Ensure modal exists in DOM
    let modal = document.getElementById('articleModal');
    if (!modal) modal = createArticleModal();

    const bsModal = new bootstrap.Modal(modal);
    document.getElementById('articleModalBody').innerHTML = `
        <div class="text-center py-4">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-2 text-muted">Loading article...</p>
        </div>`;
    bsModal.show();

    try {
        const res  = await fetch(`/api/news/${articleId}`);
        const data = await res.json();
        if (!data.success) throw new Error(data.message);
        renderArticleModal(data.data);
    } catch (err) {
        document.getElementById('articleModalBody').innerHTML = `
            <div class="alert alert-danger">Could not load article: ${err.message}</div>`;
    }
}

function createArticleModal() {
    const div = document.createElement('div');
    div.innerHTML = `
    <div class="modal fade" id="articleModal" tabindex="-1" aria-labelledby="articleModalLabel" aria-hidden="true">
      <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <div class="modal-content">
          <div class="modal-header border-0 pb-0">
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body pt-0" id="articleModalBody"></div>
          <div class="modal-footer border-0">
            <a href="/verify" class="btn btn-primary btn-sm">
                <i class="bi bi-shield-check me-1"></i>Verify This Article
            </a>
            <button class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Close</button>
          </div>
        </div>
      </div>
    </div>`;
    document.body.appendChild(div.firstElementChild);
    return document.getElementById('articleModal');
}

function renderArticleModal(article) {
    const imgHtml = article.imageUrl
        ? `<img src="${escapeHtml(article.imageUrl)}" alt="${escapeHtml(article.title)}"
               class="img-fluid rounded mb-3" style="max-height:320px;width:100%;object-fit:cover;"
               loading="lazy">`
        : '';

    document.getElementById('articleModalBody').innerHTML = `
        ${imgHtml}
        <div class="mb-2">
            <span class="badge ${article.categoryBadgeClass || 'bg-secondary'} me-2">
                ${escapeHtml(article.categoryDisplayName || article.category)}
            </span>
            <small class="text-muted">
                <i class="bi bi-calendar3 me-1"></i>${formatDate(article.publishedDate)}
                &nbsp;·&nbsp;
                <i class="bi bi-person me-1"></i>${escapeHtml(article.author || 'Staff Reporter')}
            </small>
        </div>
        <h4 class="fw-bold mb-3">${escapeHtml(article.title)}</h4>
        <p class="text-muted" style="font-size:.9rem;line-height:1.75;">${escapeHtml(article.summary || '')}</p>
        ${article.sourceUrl
            ? `<a href="${escapeHtml(article.sourceUrl)}" target="_blank" rel="noopener noreferrer"
                  class="btn btn-sm btn-outline-primary mt-2">
                   <i class="bi bi-box-arrow-up-right me-1"></i>Read Full Article at ${escapeHtml(article.sourceName || 'Source')}
               </a>` : ''}
        <div class="mt-4 pt-3 border-top">
            <button class="btn btn-sm btn-primary"
                    onclick="verifyThisArticle('${escapeHtml(article.title + '. ' + (article.summary || ''))}')">
                <i class="bi bi-shield-check me-1"></i>Verify This Article
            </button>
        </div>`;
}

function verifyThisArticle(text) {
    sessionStorage.setItem('verifyContent', text);
    window.location.href = '/verify';
}

/* ================================================================
   HELPERS
   ================================================================ */

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    try {
        const d = new Date(dateStr);
        return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    } catch (_) { return ''; }
}

function formatDateTime(dateStr) {
    if (!dateStr) return '';
    try {
        const d = new Date(dateStr);
        return d.toLocaleString('en-US', {
            year: 'numeric', month: 'short', day: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    } catch (_) { return ''; }
}

function getCategoryIcon(category) {
    const icons = {
        INTERNATIONAL: 'bi-globe',
        NATIONAL:      'bi-flag',
        POLITICS:      'bi-building',
        SPORTS:        'bi-trophy',
        TECHNOLOGY:    'bi-cpu'
    };
    return icons[category] || 'bi-newspaper';
}

function truncate(str, max = 160) {
    if (!str) return '';
    return str.length > max ? str.slice(0, max) + '…' : str;
}

/** Highlight active nav link based on current path */
document.addEventListener('DOMContentLoaded', () => {
    const path = window.location.pathname;
    document.querySelectorAll('#mainNavbar .nav-link').forEach(link => {
        const href = link.getAttribute('href');
        if (href && href !== '#' && path.startsWith(href) && href !== '/') {
            link.classList.add('active');
        } else if (href === '/' && path === '/') {
            link.classList.add('active');
        }
    });
});
