/**
 * TruthLens AI — verify.js
 * Handles all verification form submissions, loading animation,
 * and rendering of the full verification result on results.html.
 */

'use strict';

/* ================================================================
   CHAR COUNTER (verify page — text tab)
   ================================================================ */
function updateCharCount() {
    const ta = document.getElementById('textContent');
    const counter = document.getElementById('textCharCount');
    if (ta && counter) {
        const len = ta.value.length;
        counter.textContent = `${len.toLocaleString()} character${len !== 1 ? 's' : ''}`;
        counter.className = len < 20 ? 'text-danger small' : 'text-muted small';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const ta = document.getElementById('textContent');
    if (ta) {
        ta.addEventListener('input', updateCharCount);
        updateCharCount();
    }
});

/* ================================================================
   TEXT VERIFICATION
   ================================================================ */
async function submitTextVerification(e) {
    e.preventDefault();
    const content = document.getElementById('textContent').value.trim();
    if (content.length < 20) {
        showVerifyError('Please enter at least 20 characters for accurate analysis.');
        return;
    }
    hideVerifyError();
    setButtonLoading('textSubmitBtn', true);

    try {
        const res = await fetch('/api/verify/text', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ content, submissionType: 'TEXT' })
        });
        const data = await res.json();
        if (!data.success) throw new Error(data.message);
        redirectToResults(data.data);
    } catch (err) {
        showVerifyError('Verification failed: ' + err.message);
    } finally {
        setButtonLoading('textSubmitBtn', false);
    }
}

/* ================================================================
   URL VERIFICATION
   ================================================================ */
async function submitUrlVerification(e) {
    e.preventDefault();
    const url     = document.getElementById('urlInput').value.trim();
    const content = document.getElementById('urlContent').value.trim();

    if (!url) {
        showVerifyError('Please enter a URL.');
        return;
    }
    if (!isValidUrl(url)) {
        showVerifyError('Please enter a valid URL (must start with http:// or https://).');
        return;
    }
    hideVerifyError();
    setButtonLoading('urlSubmitBtn', true);

    try {
        const res = await fetch('/api/verify/url', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ url, content, submissionType: 'URL' })
        });
        const data = await res.json();
        if (!data.success) throw new Error(data.message);
        redirectToResults(data.data);
    } catch (err) {
        showVerifyError('Verification failed: ' + err.message);
    } finally {
        setButtonLoading('urlSubmitBtn', false);
    }
}

/* ================================================================
   IMAGE VERIFICATION
   ================================================================ */
async function submitImageVerification(e) {
    e.preventDefault();
    const fileInput    = document.getElementById('imageInput');
    const extractedText = document.getElementById('extractedText').value.trim();

    if (!fileInput.files.length) {
        showVerifyError('Please select an image file.');
        return;
    }
    hideVerifyError();
    setButtonLoading('imageSubmitBtn', true);

    const formData = new FormData();
    formData.append('image', fileInput.files[0]);
    formData.append('extractedText', extractedText);

    try {
        const res = await fetch('/api/verify/image', {
            method: 'POST',
            body:   formData
        });
        const data = await res.json();
        if (!data.success) throw new Error(data.message);
        redirectToResults(data.data);
    } catch (err) {
        showVerifyError('Image verification failed: ' + err.message);
    } finally {
        setButtonLoading('imageSubmitBtn', false);
    }
}

/* ================================================================
   IMAGE UPLOAD HANDLERS
   ================================================================ */
function handleImageSelect(event) {
    const file = event.target.files[0];
    if (file) previewImage(file);
}

function handleDragOver(event) {
    event.preventDefault();
    document.getElementById('imageDropzone').classList.add('dragover');
}

function handleDragLeave(event) {
    document.getElementById('imageDropzone').classList.remove('dragover');
}

function handleDrop(event) {
    event.preventDefault();
    document.getElementById('imageDropzone').classList.remove('dragover');
    const file = event.dataTransfer.files[0];
    if (file && file.type.startsWith('image/')) {
        previewImage(file);
        // Attach to file input for form submission
        const dt = new DataTransfer();
        dt.items.add(file);
        document.getElementById('imageInput').files = dt.files;
    } else {
        showVerifyError('Please drop a valid image file (JPEG, PNG, WebP, or GIF).');
    }
}

function previewImage(file) {
    const reader = new FileReader();
    reader.onload = (e) => {
        const preview = document.getElementById('imagePreview');
        const content = document.getElementById('dropzoneContent');
        if (preview && content) {
            preview.src = e.target.result;
            preview.classList.remove('d-none');
            content.classList.add('d-none');
        }
    };
    reader.readAsDataURL(file);

    const submitBtn = document.getElementById('imageSubmitBtn');
    if (submitBtn) submitBtn.disabled = false;
}

/* ================================================================
   REDIRECT TO RESULTS  (via sessionStorage)
   ================================================================ */
function redirectToResults(resultData) {
    sessionStorage.setItem('verificationResult', JSON.stringify(resultData));
    window.location.href = '/results';
}

/* ================================================================
   RESULTS PAGE — EXECUTE PENDING VERIFICATION
   (called when results.html loads with pendingVerification data)
   ================================================================ */
async function executePendingVerification(pending) {
    showLoadingState();

    try {
        let endpoint, body, options;

        if (pending.type === 'IMAGE') {
            // Image verifications aren't re-runnable via sessionStorage
            showErrorState('Image verification results cannot be reloaded. Please verify again.');
            return;
        }

        if (pending.type === 'URL') {
            endpoint = '/api/verify/url';
            body = JSON.stringify({ url: pending.url, content: pending.content, submissionType: 'URL' });
        } else {
            endpoint = '/api/verify/text';
            body = JSON.stringify({ content: pending.content, submissionType: 'TEXT' });
        }

        options = { method: 'POST', headers: { 'Content-Type': 'application/json' }, body };

        // Animate the loading steps
        await animateLoadingSteps();
        const res  = await fetch(endpoint, options);
        const data = await res.json();
        if (!data.success) throw new Error(data.message);
        displayVerificationResult(data.data);
    } catch (err) {
        showErrorState(err.message);
    }
}

/* ================================================================
   RESULTS PAGE — DISPLAY RESULT
   ================================================================ */
function displayVerificationResult(result) {
    hideLoadingState();
    document.getElementById('resultsState').classList.remove('d-none');

    // ── Verdict banner ────────────────────────────────────────────
    const banner = document.getElementById('verdictBanner');
    const cssClass = result.verdictCssClass || 'verdict-unverified';
    banner.className = `verdict-banner-section ${cssClass}`;

    setTextContent('verdictIcon',   '');
    const iconEl = document.getElementById('verdictIcon');
    if (iconEl) {
        iconEl.className = `bi ${result.verdictIcon || 'bi-question-circle-fill'}`;
    }
    setTextContent('verdictName',        result.verdictDisplayName  || result.verdict);
    setTextContent('verdictExplanation', result.explanation         || '');

    // ── Confidence gauge ──────────────────────────────────────────
    const score = result.confidenceScore || 0;
    setTextContent('confidenceScore', score);
    setTextContent('confidenceLevel', result.confidenceLabel || '');
    animateGauge(score);

    // ── Meta row ──────────────────────────────────────────────────
    setTextContent('metaCategory',
        (result.matchedCategoryDisplayName || result.matchedCategory || 'Unknown'));
    setTextContent('metaType', formatSubmissionType(result.submissionType));
    setTextContent('metaTime',
        result.analyzedAt ? formatDateTime(result.analyzedAt) : 'Just now');
    setTextContent('metaMatchCount',
        (result.matchedArticles ? result.matchedArticles.length : 0));

    // ── Submitted text preview ────────────────────────────────────
    const preview = document.getElementById('submittedTextPreview');
    if (preview) {
        preview.textContent = truncate(result.submittedText || '', 500);
    }
    if (result.submittedUrl) {
        showElement('submittedUrlBadge');
        const urlLink = document.getElementById('submittedUrlLink');
        const urlText = document.getElementById('submittedUrlText');
        if (urlLink) urlLink.href = result.submittedUrl;
        if (urlText) urlText.textContent = truncate(result.submittedUrl, 60);
    }

    // ── Matched trusted articles ──────────────────────────────────
    renderMatchedArticles(result.matchedArticles || []);

    // ── Similar news ──────────────────────────────────────────────
    renderSimilarNews(result.similarNews || []);

    // ── Summary sidebar ───────────────────────────────────────────
    renderSummary(result);

    // ── Scroll to results ─────────────────────────────────────────
    document.getElementById('resultsState').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

/* ── Matched Articles ────────────────────────────────────────── */
function renderMatchedArticles(articles) {
    const body = document.getElementById('matchedArticlesBody');
    if (!body) return;

    if (!articles.length) {
        body.innerHTML = `<div class="text-center py-4 text-muted">
            <i class="bi bi-search display-6 d-block mb-2"></i>
            No matching articles found in the trusted database.
            <div class="mt-2 small">This may be a very recent event or a niche topic not yet in our corpus.</div>
        </div>`;
        return;
    }

    body.innerHTML = articles.map((a, i) => `
        <div class="matched-article-item">
            <div class="d-flex justify-content-between align-items-start mb-2">
                <div class="flex-grow-1 me-3">
                    <h6 class="fw-bold mb-1 small">${escapeHtml(a.title)}</h6>
                    <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
                        <span class="badge bg-primary">${escapeHtml(a.sourceName || 'Unknown')}</span>
                        <span class="badge ${a.matchBadgeClass || 'bg-secondary'}">${escapeHtml(a.matchType || 'RELATED')}</span>
                        ${a.categoryDisplayName
                            ? `<span class="badge bg-light text-dark border">${escapeHtml(a.categoryDisplayName)}</span>` : ''}
                    </div>
                </div>
                <div class="text-end" style="min-width:65px">
                    <div class="fw-bold fs-5 ${getScoreColor(a.matchScore)}">${a.matchScore}%</div>
                    <div class="text-muted" style="font-size:.7rem;">match</div>
                </div>
            </div>

            <!-- Score bar -->
            <div class="match-score-bar mb-2">
                <div class="match-score-fill ${getScoreBg(a.matchScore)}"
                     style="width:0%" data-target="${a.matchScore}"
                     id="scoreBar${i}"></div>
            </div>

            <p class="text-muted mb-2" style="font-size:.82rem;line-height:1.6;">
                ${escapeHtml(truncate(a.summary || '', 200))}
            </p>

            <div class="d-flex justify-content-between align-items-center">
                <small class="text-muted">
                    ${a.author ? `<i class="bi bi-person me-1"></i>${escapeHtml(a.author)} &nbsp;·&nbsp;` : ''}
                    ${a.publishedDate ? `<i class="bi bi-calendar3 me-1"></i>${formatDate(a.publishedDate)}` : ''}
                </small>
                ${a.sourceUrl
                    ? `<a href="${escapeHtml(a.sourceUrl)}" target="_blank" rel="noopener noreferrer"
                          class="btn btn-outline-primary btn-sm py-0" style="font-size:.75rem;">
                           <i class="bi bi-box-arrow-up-right me-1"></i>Read Source
                       </a>` : ''}
            </div>

            <div class="mt-2 pt-2 border-top d-flex align-items-center gap-2">
                <small class="text-muted">Source Credibility:</small>
                <div class="progress flex-grow-1" style="height:5px;">
                    <div class="progress-bar bg-success" style="width:${a.credibilityScore || 0}%"></div>
                </div>
                <small class="fw-bold text-success">${a.credibilityScore || 0}/100</small>
            </div>
        </div>
    `).join('');

    // Animate score bars after render
    requestAnimationFrame(() => {
        document.querySelectorAll('.match-score-fill[data-target]').forEach(bar => {
            setTimeout(() => {
                bar.style.width = bar.dataset.target + '%';
            }, 200);
        });
    });
}

/* ── Similar News ────────────────────────────────────────────── */
function renderSimilarNews(articles) {
    const body = document.getElementById('similarNewsBody');
    if (!body) return;

    if (!articles.length) {
        body.innerHTML = `<div class="text-center text-muted py-3 small">
            <i class="bi bi-newspaper me-1"></i>No similar articles available.</div>`;
        return;
    }

    body.innerHTML = `<div class="row gy-3">
        ${articles.slice(0, 6).map(a => `
            <div class="col-md-6">
                <div class="border rounded p-3 h-100" style="cursor:pointer;transition:all .2s;"
                     onmouseenter="this.style.borderColor='var(--primary)';this.style.background='var(--primary-light)'"
                     onmouseleave="this.style.borderColor='';this.style.background=''"
                     onclick="showArticleModal(${a.id})">
                    <span class="badge ${a.categoryBadgeClass || 'bg-secondary'} mb-2" style="font-size:.68rem;">
                        ${escapeHtml(a.categoryDisplayName || a.category)}
                    </span>
                    <p class="fw-semibold mb-1 small" style="line-height:1.35;">
                        ${escapeHtml(truncate(a.title, 90))}
                    </p>
                    <small class="text-muted d-flex align-items-center gap-2">
                        <i class="bi bi-newspaper"></i>${escapeHtml(a.sourceName || 'Unknown')}
                        ${a.publishedDate ? `<span>&middot; ${formatDate(a.publishedDate)}</span>` : ''}
                    </small>
                </div>
            </div>`).join('')}
    </div>`;
}

/* ── Summary sidebar ─────────────────────────────────────────── */
function renderSummary(result) {
    setTextContent('summaryVerdict', result.verdictDisplayName || result.verdict || '–');
    setTextContent('summaryConfidence', `${result.confidenceScore || 0}% (${result.confidenceLabel || '–'})`);
    setTextContent('summaryCategory', result.matchedCategoryDisplayName || result.matchedCategory || '–');
    setTextContent('summaryMatchCount', (result.matchedArticles ? result.matchedArticles.length : 0) + ' source(s)');
    setTextContent('summaryType', formatSubmissionType(result.submissionType));
    setTextContent('summaryTime', result.analyzedAt ? formatDateTime(result.analyzedAt) : 'Just now');
}

/* ================================================================
   LOADING ANIMATION  (results page)
   ================================================================ */
function showLoadingState() {
    showElement('loadingState');
    hideElement('resultsState');
    hideElement('errorState');
    animateLoadingSteps();
}

async function animateLoadingSteps() {
    const steps = [
        { id: 'step1', label: 'Extracting keywords',        progress: 25  },
        { id: 'step2', label: 'Detecting category',         progress: 50  },
        { id: 'step3', label: 'Searching trusted sources',  progress: 75  },
        { id: 'step4', label: 'Computing verdict',          progress: 95  }
    ];
    const bar = document.getElementById('analysisProgressBar');

    for (const step of steps) {
        await sleep(400);
        const el = document.getElementById(step.id);
        if (el) {
            el.classList.add('active');
            el.querySelector('i').className = 'bi bi-circle-fill me-1';
        }
        if (bar) bar.style.width = step.progress + '%';
    }

    await sleep(300);
    // Mark all done
    steps.forEach(s => {
        const el = document.getElementById(s.id);
        if (el) {
            el.classList.remove('active');
            el.classList.add('done');
            el.querySelector('i').className = 'bi bi-check-circle-fill me-1';
        }
    });
    if (bar) bar.style.width = '100%';
}

function hideLoadingState() {
    hideElement('loadingState');
}

function showErrorState(message) {
    hideElement('loadingState');
    hideElement('resultsState');
    showElement('errorState');
    setTextContent('errorMessage', message || 'An unexpected error occurred.');
}

/* ================================================================
   CONFIDENCE GAUGE ANIMATION
   ================================================================ */
function animateGauge(score) {
    const fill = document.getElementById('gaugeFill');
    if (!fill) return;
    const circumference = 314; // 2 * π * r (r=50)
    const offset = circumference - (score / 100) * circumference;
    setTimeout(() => {
        fill.style.transition = 'stroke-dashoffset 1s ease';
        fill.style.strokeDashoffset = offset;
    }, 300);
}

/* ================================================================
   RESULT ACTIONS
   ================================================================ */
function copyResultLink() {
    const url = window.location.href;
    if (navigator.clipboard) {
        navigator.clipboard.writeText(url).then(() => {
            showToast('Link copied to clipboard!', 'success');
        }).catch(() => fallbackCopy(url));
    } else {
        fallbackCopy(url);
    }
}

function fallbackCopy(text) {
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.cssText = 'position:fixed;opacity:0;';
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
    showToast('Link copied!', 'success');
}

function printResult() {
    window.print();
}

/* ================================================================
   TOAST NOTIFICATION
   ================================================================ */
function showToast(message, type = 'info') {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.style.cssText = 'position:fixed;top:80px;right:20px;z-index:9999;';
        document.body.appendChild(container);
    }
    const id = 'toast_' + Date.now();
    const colorMap = { success: 'bg-success', error: 'bg-danger', info: 'bg-primary', warning: 'bg-warning' };
    const bg = colorMap[type] || 'bg-primary';
    const div = document.createElement('div');
    div.innerHTML = `
        <div id="${id}" class="toast align-items-center text-white ${bg} border-0 mb-2" role="alert">
          <div class="d-flex">
            <div class="toast-body fw-semibold">${escapeHtml(message)}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto"
                    data-bs-dismiss="toast"></button>
          </div>
        </div>`;
    container.appendChild(div.firstElementChild);
    const toastEl = document.getElementById(id);
    const bsToast = new bootstrap.Toast(toastEl, { delay: 3000 });
    bsToast.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}

/* ================================================================
   FORM ERROR HELPERS (verify page)
   ================================================================ */
function showVerifyError(message) {
    const el = document.getElementById('verifyErrorAlert');
    const msg = document.getElementById('verifyErrorMessage');
    if (el && msg) {
        msg.textContent = message;
        el.classList.remove('d-none');
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
}

function hideVerifyError() {
    const el = document.getElementById('verifyErrorAlert');
    if (el) el.classList.add('d-none');
}

function setButtonLoading(btnId, loading) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    const textSpan    = btn.querySelector('.btn-text');
    const loadingSpan = btn.querySelector('.btn-loading');
    btn.disabled = loading;
    if (textSpan)    textSpan.classList.toggle('d-none', loading);
    if (loadingSpan) loadingSpan.classList.toggle('d-none', !loading);
}

/* ================================================================
   DOM UTILITIES
   ================================================================ */
function setTextContent(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value ?? '';
}

function showElement(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('d-none');
}

function hideElement(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('d-none');
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/* ================================================================
   FORMATTING HELPERS
   ================================================================ */
function formatSubmissionType(type) {
    const map = { TEXT: 'Text / Article', URL: 'News URL', IMAGE: 'Image' };
    return map[type] || type || '–';
}

function getScoreColor(score) {
    if (score >= 75) return 'text-success';
    if (score >= 45) return 'text-warning';
    return 'text-secondary';
}

function getScoreBg(score) {
    if (score >= 75) return 'bg-success';
    if (score >= 45) return 'bg-warning';
    return 'bg-secondary';
}

function isValidUrl(str) {
    try {
        const url = new URL(str);
        return url.protocol === 'http:' || url.protocol === 'https:';
    } catch (_) { return false; }
}

function truncate(str, max = 160) {
    if (!str) return '';
    return str.length > max ? str.slice(0, max) + '…' : str;
}

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
        return new Date(dateStr).toLocaleDateString('en-US',
            { year: 'numeric', month: 'short', day: 'numeric' });
    } catch (_) { return ''; }
}

function formatDateTime(dateStr) {
    if (!dateStr) return '';
    try {
        return new Date(dateStr).toLocaleString('en-US', {
            year: 'numeric', month: 'short', day: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    } catch (_) { return ''; }
}
