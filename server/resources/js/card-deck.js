function element(tag, className = '') {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }

    return node;
}

function applyDataset(node, dataset = {}) {
    for (const [key, value] of Object.entries(dataset)) {
        if (value !== null && value !== undefined) {
            node.dataset[key] = String(value);
        }
    }
}

function setFaceState(card, flipped) {
    const front = card.querySelector('.mp-deck-face--front');
    const back = card.querySelector('.mp-deck-face--back');
    if (!front || !back) {
        return;
    }

    const apply = (face, hidden) => {
        face.setAttribute('aria-hidden', hidden ? 'true' : 'false');
        if (hidden) {
            face.setAttribute('inert', '');
        } else {
            face.removeAttribute('inert');
        }
    };

    apply(front, flipped);
    apply(back, !flipped);
}

export function createCompactCard({ className = '', dataset = {}, content }) {
    const card = element('button', ['mp-deck-card', 'mp-deck-card--compact', className].filter(Boolean).join(' '));
    card.type = 'button';
    applyDataset(card, dataset);
    if (content) {
        card.appendChild(content);
    }

    return card;
}

export function createFlipCard({ className = '', faceClassName = '', dataset = {}, front, back, flipped = false }) {
    const card = element('article', ['mp-deck-card', 'mp-deck-card--flip', className].filter(Boolean).join(' '));
    applyDataset(card, dataset);

    const inner = element('div', 'mp-deck-inner');
    const frontFace = element('div', ['mp-deck-face', 'mp-deck-face--front', faceClassName].filter(Boolean).join(' '));
    const backFace = element('div', ['mp-deck-face', 'mp-deck-face--back', faceClassName].filter(Boolean).join(' '));
    frontFace.appendChild(front);
    backFace.appendChild(back);
    inner.append(frontFace, backFace);
    card.appendChild(inner);
    card.classList.toggle('is-flipped', Boolean(flipped));
    setFaceState(card, Boolean(flipped));

    window.requestAnimationFrame(() => syncDeckHeight(card));

    return card;
}

export function syncDeckHeight(card) {
    if (!card?.classList.contains('mp-deck-card--flip')) {
        return;
    }

    const flipped = card.classList.contains('is-flipped');
    const face = card.querySelector(flipped ? '.mp-deck-face--back' : '.mp-deck-face--front');
    const height = face?.scrollHeight ?? 0;
    if (height > 0) {
        card.style.setProperty('--deck-height', `${height}px`);
    }
}

export function setDeckFlipped(card, flipped, { focus = true } = {}) {
    if (!card?.classList.contains('mp-deck-card--flip')) {
        return;
    }

    card.classList.toggle('is-flipped', Boolean(flipped));
    setFaceState(card, Boolean(flipped));
    syncDeckHeight(card);

    if (!focus) {
        return;
    }

    const target = card.querySelector(
        flipped
            ? '.mp-deck-face--back [data-deck-flip]'
            : '.mp-deck-face--front [data-deck-flip]',
    );
    window.setTimeout(() => target?.focus({ preventScroll: true }), 60);
}

export function syncAllDeckHeights(root = document) {
    root.querySelectorAll('.mp-deck-card--flip').forEach((card) => syncDeckHeight(card));
}

let resizeTimer = null;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => syncAllDeckHeights(), 120);
});
document.fonts?.ready?.then(() => syncAllDeckHeights()).catch(() => {});
