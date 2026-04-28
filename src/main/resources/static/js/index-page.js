const refreshBtn = document.getElementById('refreshBtn');
const sourceBadge = document.getElementById('sourceBadge');
const meta = document.getElementById('meta');
const subtitle = document.getElementById('subtitle');
const list = document.getElementById('list');
const INITIAL_ITEMS_PER_GROUP = 2;
const LOAD_MORE_STEP = 2;
const cardTypes = [
	{ key: 'security', label: 'Security watch' },
	{ key: 'release', label: 'Release impact' },
	{ key: 'signal', label: 'Ecosystem signal' }
];
let groupedItems = { security: [], release: [], signal: [] };
let visibleCountByType = { security: 0, release: 0, signal: 0 };

function escapeHtml(input) {
	return String(input ?? '')
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;')
		.replace(/'/g, '&#39;');
}

function normalizeDidYouKnowText(value) {
	const fallback = 'Interesting context is not ready yet. Check back after refresh.';
	const text = String(value ?? '').trim();
	if (!text) {
		return fallback;
	}
	return text
		.replace(/^did you know\??\s*[:\-]?\s*/i, '')
		.replace(/^prediction\s*[:\-]?\s*/i, '')
		.trim() || fallback;
}

function detectType(item) {
	const text = `${item.title || ''} ${item.summary || ''} ${item.action || ''} ${item.didYouKnow || ''} ${item.url || ''}`.toLowerCase();
	if (/(security|cve|vulnerability|vuln|advisory|exploit|zero-day|xss|csrf|rce|auth bypass|patch)/.test(text)) {
		return 'security';
	}
	if (/(release|releases|launch|version|v\d+\.\d+|changelog|ga\b|beta|alpha|rc\b|stable|rollout|now available|upgrade|updated|tag)/.test(text)) {
		return 'release';
	}
	return 'signal';
}

function dedupeByUrl(items) {
	const seen = new Set();
	return items.filter(item => {
		if (!item || !item.url) {
			return false;
		}
		if (seen.has(item.url)) {
			return false;
		}
		seen.add(item.url);
		return true;
	});
}

function buildGroups(items) {
	const groups = { security: [], release: [], signal: [] };
	for (const item of dedupeByUrl(items)) {
		groups[detectType(item)].push(item);
	}

	const minPerGroup = Math.min(INITIAL_ITEMS_PER_GROUP, Math.floor((groups.security.length + groups.release.length + groups.signal.length) / cardTypes.length));
	if (minPerGroup > 0) {
		for (const receiverType of cardTypes) {
			while (groups[receiverType.key].length < minPerGroup) {
				const donor = cardTypes
					.map(type => type.key)
					.filter(typeKey => typeKey !== receiverType.key)
					.sort((a, b) => groups[b].length - groups[a].length)
					.find(typeKey => groups[typeKey].length > minPerGroup);

				if (!donor) {
					break;
				}
				groups[receiverType.key].push(groups[donor].pop());
			}
		}
	}
	return groups;
}

function renderCard(item, typeKey, typeLabel) {
	const card = document.createElement('article');
	card.className = `card ${typeKey}`;

	if (!item) {
		card.innerHTML = `
			<span class="card-label ${typeKey}">${typeLabel}</span>
			<h3>No matching item</h3>
			<p class="item-summary">No updates are available in this lane right now.</p>
		`;
		return card;
	}

	card.innerHTML = `
		<span class="card-label ${typeKey}">${typeLabel}</span>
		<h3>${escapeHtml(item.title)}</h3>
		<p class="item-meta">${escapeHtml(item.source)} • ${new Date(item.publishedAt).toLocaleString()}</p>
		<p class="item-summary">${escapeHtml(item.summary)}</p>
		<p class="item-action">${escapeHtml(item.action)}</p>
		<p class="item-did-you-know"><strong>Did you know:</strong> ${escapeHtml(normalizeDidYouKnowText(item.didYouKnow))}</p>
		<p><a class="link-btn" href="${escapeHtml(item.url)}" target="_blank" rel="noopener noreferrer">Read source</a></p>
	`;
	return card;
}

function renderSections() {
	list.innerHTML = '';

	for (const type of cardTypes) {
		const sectionEl = document.createElement('section');
		sectionEl.className = 'section';

		const allItems = groupedItems[type.key] || [];
		const visible = visibleCountByType[type.key] || 0;
		const visibleItems = allItems.slice(0, visible);
		const shownCount = visibleItems.length;

		sectionEl.innerHTML = `
			<div class="section-head">
				<h2>${type.label}</h2>
				<span class="hint">${shownCount} of ${allItems.length}</span>
			</div>
		`;

		const gridEl = document.createElement('div');
		gridEl.className = 'grid';

		if (shownCount === 0) {
			gridEl.appendChild(renderCard(null, type.key, type.label));
		} else {
			for (const item of visibleItems) {
				gridEl.appendChild(renderCard(item, type.key, type.label));
			}
		}

		sectionEl.appendChild(gridEl);

		if (shownCount < allItems.length) {
			const controlsEl = document.createElement('div');
			controlsEl.className = 'section-controls';
			const remaining = allItems.length - shownCount;
			const button = document.createElement('button');
			button.type = 'button';
			button.className = 'load-more-btn';
			button.textContent = `Load more (${remaining})`;
			button.addEventListener('click', () => {
				visibleCountByType[type.key] = Math.min(allItems.length, shownCount + LOAD_MORE_STEP);
				renderSections();
			});
			controlsEl.appendChild(button);
			sectionEl.appendChild(controlsEl);
		}

		list.appendChild(sectionEl);
	}
}

async function loadUpdates() {
	try {
		const response = await fetch('/api/updates?limit=20');
		const data = await response.json();
		const itemCount = Array.isArray(data.items) ? data.items.length : 0;
		subtitle.textContent = `Live brief: ${itemCount} high-signal updates from ${data.source}.`;
		const freshness = data.fromCache
			? 'cached at ' + new Date(data.cachedAt).toLocaleString()
			: 'freshly generated';
		const llmNotice = data.llmPending
			? (data.llmMessage || 'Local LLM is still generating insights. Showing available updates now.')
			: '';
		meta.textContent = 'Source: ' + data.source + ' | Generated: ' + new Date(data.generatedAt).toLocaleString() + ' | ' + freshness + (llmNotice ? ' | ' + llmNotice : '');
		list.innerHTML = '';

		if (!data.items || data.items.length === 0) {
			list.innerHTML = '<article class="card"><h3>No updates yet</h3><p>Try refreshing in a moment.</p></article>';
			return;
		}

		groupedItems = buildGroups(data.items);
		visibleCountByType = {
			security: Math.min(INITIAL_ITEMS_PER_GROUP, groupedItems.security.length),
			release: Math.min(INITIAL_ITEMS_PER_GROUP, groupedItems.release.length),
			signal: Math.min(INITIAL_ITEMS_PER_GROUP, groupedItems.signal.length)
		};
		renderSections();
	} catch (error) {
		subtitle.textContent = 'Live brief is temporarily unavailable. Retry to refresh the latest signals.';
		meta.textContent = 'Could not load updates right now.';
		list.innerHTML = '<article class="card"><h3>Temporarily unavailable</h3><p>Please try again in a moment.</p></article>';
	}
}

async function loadHealth() {
	try {
		const response = await fetch('/api/health/updates');
		const health = await response.json();
		if (health.source && health.source.available) {
			sourceBadge.className = 'badge ok';
			sourceBadge.textContent = 'Feed source: online';
		} else {
			sourceBadge.className = 'badge down';
			sourceBadge.textContent = 'Feed source: offline';
		}
	} catch (error) {
		sourceBadge.className = 'badge unknown';
		sourceBadge.textContent = 'Feed source: unknown';
	}
}

async function refreshNow() {
	refreshBtn.disabled = true;
	refreshBtn.classList.add('is-loading');
	try {
		await fetch('/api/updates/refresh-all', { method: 'POST' });
		await Promise.all([loadUpdates(), loadHealth()]);
	} finally {
		refreshBtn.classList.remove('is-loading');
		refreshBtn.disabled = false;
	}
}

refreshBtn.addEventListener('click', refreshNow);
loadUpdates();
loadHealth();
setInterval(loadHealth, 30000);

