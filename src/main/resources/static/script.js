const API = '/api';
const AUTH_KEY = 'gm_admin_auth';

const ROLES = ['Student', 'Teacher', 'Admin'];
const AOES = ['SINGLE', 'PARTY', 'GUILD'];
const STATUSES = ['PENDING', 'APPROVED', 'REJECTED'];
const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
const LEVELS = ['ESO', 'Bachillerato', 'FPBásica', 'FPMedia', 'FPSuperior'];
const MODALITIES = ['Ciencias', 'Letras', 'Humanidades', 'Tecnologico', 'DAM', 'SMR', 'DAW', 'ASIR'];
const JOBS = ['Mage', 'Rogue', 'Paladin'];
const CURSOS = ['1', '2', '3', '4'];
/** Sentinel for optional combo --Empty-- (stored as NULL in DB) */
const EMPTY_SENTINEL = '__EMPTY__';

function isEmptyComboValue(v) {
    return v == null || v === '' || v === EMPTY_SENTINEL;
}

function nullHtml() {
    return '<span class="null-value">NULL</span>';
}
function cellOrNull(value, display) {
    if (value == null || value === '') return nullHtml();
    return display != null ? display : String(value);
}

function idName(id, name) {
    if (id == null || id === '') return '';
    return `${id}. ${name || '?'}`;
}
function guildLabel(g) {
    if (!g) return '';
    const parts = [g.number, g.letter, g.level, g.modality].filter(v => v != null && String(v).trim() !== '');
    const detail = parts.join(' ');
    return `${g.id}. ${g.name || '?'}${detail ? ` (${detail})` : ''}`;
}
function userById(id) { return usersCache.find(u => u.id === id); }
function guildById(id) { return guildsCache.find(g => g.id === id); }
function partyById(id) { return partiesCache.find(p => p.id === id); }
function characterById(id) { return charactersCache.find(c => c.id === id); }
function skillById(id) { return skillsCache.find(s => s.id === id); }
function teacherOpts() {
    return usersCache
        .filter(u => u.role === 'Teacher')
        .map(u => ({ value: u.id, label: idName(u.id, u.name) }));
}
function userOpts() {
    return usersCache.map(u => ({ value: u.id, label: idName(u.id, u.name) }));
}
function guildOpts() {
    return guildsCache.map(g => ({ value: g.id, label: guildLabel(g) }));
}
function partyOpts() {
    return partiesCache.map(p => ({ value: p.id, label: idName(p.id, p.name) }));
}
function partyOptsForGuild(guildId) {
    if (guildId == null || guildId === '') return [];
    const gid = Number(guildId);
    return partiesCache
        .filter(p => Number(p.guild_id) === gid)
        .map(p => ({ value: p.id, label: idName(p.id, p.name) }));
}
function refreshCharPartyCombo(clearIfInvalid = true) {
    const guildId = getComboValueOrNull('combo_insCharGuildId');
    const opts = partyOptsForGuild(guildId);
    setComboOptions('combo_insCharPartyId', opts);
    if (!clearIfInvalid) return;
    const current = getComboValueOrNull('combo_insCharPartyId');
    if (current != null && !opts.some(o => Number(o.value) === Number(current))) {
        setComboValue('combo_insCharPartyId', null, '');
    }
}
function characterOpts() {
    return charactersCache.map(c => ({ value: c.id, label: idName(c.id, c.name) }));
}
function skillOpts() {
    return skillsCache.map(s => ({ value: s.id, label: idName(s.id, s.name) }));
}
function jobOpts() {
    return JOBS.map(j => ({ value: j, label: j }));
}

let editContext = null;
let activeTab = 'users';
let usersCache = [];
let guildsCache = [];
let partiesCache = [];
let charactersCache = [];
let skillsCache = [];
const combos = {};

function notify(message, type = 'ok') {
    let el = document.getElementById('gmToast');
    if (!el) {
        el = document.createElement('div');
        el.id = 'gmToast';
        document.body.appendChild(el);
    }
    el.textContent = message;
    el.className = `gm-toast ${type} show`;
    clearTimeout(notify._timer);
    notify._timer = setTimeout(() => el.classList.remove('show'), 2200);
}

/** Reload table(s) after CRUD without leaving the current tab or scrolling to top. */
async function afterMutation(...loaders) {
    const tab = activeTab;
    const y = window.scrollY;
    for (const fn of loaders) {
        if (typeof fn === 'function') await fn();
    }
    refreshRelationCombos();
    reapplyFilters();
    activarTab(tab);
    requestAnimationFrame(() => window.scrollTo(0, y));
}

document.addEventListener('DOMContentLoaded', () => {
    const sesion = sessionStorage.getItem(AUTH_KEY);
    if (!sesion) {
        window.location.replace('/login.html');
        setTimeout(() => {
            const g = document.getElementById('authGuard');
            if (g) g.style.display = 'block';
        }, 800);
        return;
    }

    const admin = JSON.parse(sesion);
    document.getElementById('app').style.display = 'block';
    document.getElementById('adminSesion').textContent = `Admin: ${admin.name}`;
    document.getElementById('btnLogout').addEventListener('click', () => {
        sessionStorage.removeItem(AUTH_KEY);
        window.location.href = '/login.html';
    });

    document.querySelectorAll('input[type="number"]').forEach(inp => {
        const isExp = /exp/i.test(inp.id || '');
        if (!inp.hasAttribute('min')) inp.min = isExp ? '0' : '1';
        inp.addEventListener('input', () => {
            if (inp.value === '') return;
            const min = Number(inp.min || (isExp ? 0 : 1));
            if (Number(inp.value) < min) inp.value = String(min);
        });
    });

    initTableFilters();
    initStaticCombos();
    configurarFormularios();
    cargarTodo();
});

/* ========== TABLE FILTERS ========== */
function initTableFilters() {
    document.querySelectorAll('.table-wrap').forEach(wrap => {
        const tbodyId = wrap.dataset.table;
        const inputs = wrap.querySelectorAll('.table-filter');
        inputs.forEach(inp => {
            inp.addEventListener('input', () => {
                const q = inp.value;
                inputs.forEach(other => { if (other !== inp) other.value = q; });
                filtrarTabla(tbodyId, q);
            });
        });
    });
}

function filtrarTabla(tbodyId, query) {
    const q = (query || '').trim().toLowerCase();
    const tbody = document.getElementById(tbodyId);
    if (!tbody) return;
    tbody.querySelectorAll('tr').forEach(tr => {
        const text = tr.textContent.toLowerCase();
        tr.classList.toggle('filtered-out', q !== '' && !text.includes(q));
    });
}

function reapplyFilters() {
    document.querySelectorAll('.table-wrap').forEach(wrap => {
        const inp = wrap.querySelector('.table-filter');
        if (inp) filtrarTabla(wrap.dataset.table, inp.value);
    });
}

/* ========== SEARCHABLE COMBO ========== */
function createCombo(containerId, { allowEmpty = false, emptyLabel = '--Empty--', freeText = false, onChange = null } = {}) {
    const host = document.getElementById(containerId);
    if (!host) return null;

    const hidden = document.createElement('input');
    hidden.type = 'hidden';
    hidden.id = containerId.replace(/^combo_/, '');

    const search = document.createElement('input');
    search.type = 'text';
    search.className = 'combo-search combo-empty';
    search.placeholder = 'Type to filter...';
    search.autocomplete = 'off';

    const list = document.createElement('div');
    list.className = 'combo-list';

    host.innerHTML = '';
    host.classList.add('combo', 'combo-empty');
    host.appendChild(hidden);
    host.appendChild(search);
    host.appendChild(list);

    const state = { options: [], allowEmpty, emptyLabel, freeText, onChange, search, hidden, list };

    function render(filter = '') {
        const q = filter.trim().toLowerCase();
        let opts = state.options;
        if (q) opts = opts.filter(o => o.label.toLowerCase().includes(q) || String(o.value).toLowerCase().includes(q));
        list.innerHTML = '';
        if (allowEmpty) {
            const item = document.createElement('div');
            item.className = 'combo-item';
            item.textContent = emptyLabel;
            item.addEventListener('mousedown', e => {
                e.preventDefault();
                setComboValue(containerId, EMPTY_SENTINEL, emptyLabel);
                list.classList.remove('open');
            });
            list.appendChild(item);
        }
        opts.forEach(o => {
            const item = document.createElement('div');
            item.className = 'combo-item';
            item.textContent = o.label;
            item.addEventListener('mousedown', e => {
                e.preventDefault();
                setComboValue(containerId, o.value, o.label);
                list.classList.remove('open');
            });
            list.appendChild(item);
        });
        if (!opts.length && !allowEmpty) {
            const empty = document.createElement('div');
            empty.className = 'combo-item';
            empty.textContent = 'No matches';
            list.appendChild(empty);
        }
    }

    // Show full list on open; filter only while typing
    search.addEventListener('focus', () => {
        render('');
        list.classList.add('open');
    });
    search.addEventListener('input', () => {
        if (freeText) {
            hidden.value = search.value;
        } else if (search.value === '') {
            hidden.value = allowEmpty ? EMPTY_SENTINEL : '';
        }
        render(search.value);
        list.classList.add('open');
    });
    search.addEventListener('blur', () => {
        setTimeout(() => list.classList.remove('open'), 150);
        if (!freeText) {
            if (allowEmpty && isEmptyComboValue(hidden.value)) {
                hidden.value = EMPTY_SENTINEL;
                search.value = emptyLabel;
                return;
            }
            const match = state.options.find(o => o.label === search.value || String(o.value) === search.value);
            if (!match && search.value !== '') {
                if (hidden.value === '') search.value = '';
                else {
                    const cur = state.options.find(o => String(o.value) === String(hidden.value));
                    search.value = cur ? cur.label : '';
                }
            }
        }
    });
    // Enter inside combo must NOT submit the parent form (that reloads the page → Users tab)
    search.addEventListener('keydown', e => {
        if (e.key !== 'Enter') return;
        e.preventDefault();
        e.stopPropagation();
        const first = list.querySelector('.combo-item');
        if (first && list.classList.contains('open')) {
            first.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
        }
    });

    combos[containerId] = state;
    return state;
}

function setComboOptions(containerId, options) {
    const c = combos[containerId];
    if (!c) return;
    c.options = options || [];
}

function setComboValue(containerId, value, label) {
    const c = combos[containerId];
    if (!c) return;
    const prev = c.hidden.value;
    const host = document.getElementById(containerId);
    if (isEmptyComboValue(value)) {
        c.hidden.value = c.allowEmpty ? EMPTY_SENTINEL : '';
        c.search.value = label != null && label !== '' ? label : (c.allowEmpty ? c.emptyLabel : '');
    } else {
        c.hidden.value = String(value);
        c.search.value = label != null ? label : String(value);
    }
    const unselected = c.hidden.value === '' || (c.search.value === '' && isEmptyComboValue(c.hidden.value));
    c.search.classList.toggle('combo-empty', unselected);
    if (host) host.classList.toggle('combo-empty', unselected);
    if (typeof c.onChange === 'function' && prev !== c.hidden.value) {
        c.onChange(isEmptyComboValue(c.hidden.value) ? null : c.hidden.value);
    }
}

function getComboValue(containerId) {
    const c = combos[containerId];
    if (!c) return '';
    return c.hidden.value;
}

function getComboValueOrNull(containerId) {
    const v = getComboValue(containerId);
    if (isEmptyComboValue(v)) return null;
    return Number.isNaN(Number(v)) ? v : Number(v);
}

function initStaticCombos() {
    createCombo('combo_insUserRole');
    setComboOptions('combo_insUserRole', ROLES.map(r => ({ value: r, label: r })));

    createCombo('combo_insGuildNumber');
    setComboOptions('combo_insGuildNumber', CURSOS.map(n => ({ value: n, label: n })));

    createCombo('combo_insGuildLetter');
    setComboOptions('combo_insGuildLetter', LETTERS.map(l => ({ value: l, label: l })));

    createCombo('combo_insGuildLevel', { allowEmpty: true, emptyLabel: '--Empty--' });
    setComboOptions('combo_insGuildLevel', LEVELS.map(l => ({ value: l, label: l })));

    createCombo('combo_insGuildModality', { allowEmpty: true, emptyLabel: '--Empty--' });
    setComboOptions('combo_insGuildModality', MODALITIES.map(m => ({ value: m, label: m })));

    createCombo('combo_insMentUserId');
    createCombo('combo_insMentGuildId');
    createCombo('combo_insPartyGuildId');
    createCombo('combo_insCharJob');
    setComboOptions('combo_insCharJob', jobOpts());
    createCombo('combo_insCharLevel');
    setComboOptions('combo_insCharLevel', CURSOS.map(n => ({ value: n, label: n })));
    createCombo('combo_insCharUserId');
    createCombo('combo_insCharGuildId', {
        onChange: () => refreshCharPartyCombo(true)
    });
    createCombo('combo_insCharPartyId', { allowEmpty: true, emptyLabel: '--Empty--' });
    refreshCharPartyCombo(false);
    createCombo('combo_insSkillJob');
    setComboOptions('combo_insSkillJob', jobOpts());
    createCombo('combo_insSkillLevelReq');
    setComboOptions('combo_insSkillLevelReq', CURSOS.map(n => ({ value: n, label: n })));
    createCombo('combo_insSkillAoe');
    setComboOptions('combo_insSkillAoe', AOES.map(a => ({ value: a, label: a })));

    createCombo('combo_insEvCaster');
    createCombo('combo_insEvSkill');
    createCombo('combo_insEvGuild');
    createCombo('combo_insEvTargetChar', { allowEmpty: true, emptyLabel: '--Empty--' });
    createCombo('combo_insEvTargetParty', { allowEmpty: true, emptyLabel: '--Empty--' });
    createCombo('combo_insEvStatus');
    setComboOptions('combo_insEvStatus', STATUSES.map(s => ({ value: s, label: s })));
    if (combos.combo_insEvStatus) {
        combos.combo_insEvStatus.onChange = () => syncInsertEventCommentLabel();
    }
    syncInsertEventCommentLabel();
    createCombo('combo_insEvReviewer', { allowEmpty: true, emptyLabel: '--Empty--' });
}

function refreshRelationCombos() {
    setComboOptions('combo_insMentUserId', teacherOpts());
    setComboOptions('combo_insMentGuildId', guildOpts());
    setComboOptions('combo_insPartyGuildId', guildOpts());
    setComboOptions('combo_insCharUserId', userOpts());
    setComboOptions('combo_insCharGuildId', guildOpts());
    refreshCharPartyCombo(true);
    setComboOptions('combo_insCharJob', jobOpts());
    setComboOptions('combo_insSkillJob', jobOpts());
    setComboOptions('combo_insEvCaster', characterOpts());
    setComboOptions('combo_insEvSkill', skillOpts());
    setComboOptions('combo_insEvGuild', guildOpts());
    setComboOptions('combo_insEvTargetChar', characterOpts());
    setComboOptions('combo_insEvTargetParty', partyOpts());
    setComboOptions('combo_insEvReviewer', teacherOpts());
}

function createEditCombo(key, options, { value, allowEmpty = false, emptyLabel = '(None)', freeText = false } = {}) {
    const wrap = document.createElement('div');
    wrap.className = 'form-group';
    const label = document.createElement('label');
    label.textContent = key;
    const host = document.createElement('div');
    host.id = `combo_edit_${key}`;
    wrap.appendChild(label);
    wrap.appendChild(host);
    // temporarily in DOM for createCombo - will append to editFields
    document.getElementById('editFields').appendChild(wrap);
    createCombo(host.id, { allowEmpty, emptyLabel, freeText });
    setComboOptions(host.id, options);
    if (value != null && value !== '') {
        const opt = options.find(o => String(o.value) === String(value));
        setComboValue(host.id, value, opt ? opt.label : String(value));
    } else if (allowEmpty) {
        setComboValue(host.id, null, '');
    }
    return host.id;
}

window.activarTab = function (tabId) {
    activeTab = tabId;
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab').forEach(el => el.classList.remove('active'));
    document.getElementById(`tab-${tabId}`)?.classList.add('active');
    document.querySelector(`.tab[data-tab="${tabId}"]`)?.classList.add('active');
};

window.cerrarModal = function () {
    document.getElementById('modalEdit').style.display = 'none';
    editContext = null;
};

async function api(path, options = {}) {
    const res = await fetch(`${API}${path}`, {
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
        ...options
    });
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = text; }
    return { ok: res.ok, status: res.status, data };
}

function val(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : '';
}

function assertNumberMins(specs) {
    for (const { id, min } of specs) {
        const v = val(id);
        if (v === '' || Number(v) < min) {
            notify(`Value must be at least ${min}`, 'err');
            return false;
        }
    }
    return true;
}

function requireCombo(comboId, label) {
    const v = getComboValue(comboId);
    if (isEmptyComboValue(v)) {
        notify(`${label} is required`, 'err');
        return false;
    }
    return true;
}

/** Comment is required only when Status is APPROVED or REJECTED. */
function commentRequiredForStatus(status) {
    return status != null && status !== '' && status !== 'PENDING';
}

function setCommentLabelRequired(labelEl, required) {
    if (!labelEl) return;
    labelEl.innerHTML = required
        ? 'Comment<span class="req">*</span>'
        : 'Comment';
}

function syncInsertEventCommentLabel() {
    const status = getComboValue('combo_insEvStatus');
    setCommentLabelRequired(
        document.getElementById('lblInsEvComment'),
        commentRequiredForStatus(status)
    );
}

function assertCommentForStatus(status, comment) {
    if (commentRequiredForStatus(status) && (!comment || !String(comment).trim())) {
        notify('Comment is required when Status is APPROVED or REJECTED', 'err');
        return false;
    }
    return true;
}


async function cargarTodo() {
    await Promise.all([cargarUsers(), cargarGuilds(), cargarSkills()]);
    await cargarParties();
    await cargarCharacters();
    await Promise.all([cargarMentorships(), cargarEvents()]);
    refreshRelationCombos();
    reapplyFilters();
}

function configurarFormularios() {
    document.getElementById('formInsertUser').addEventListener('submit', async e => {
        e.preventDefault();
        if (!requireCombo('combo_insUserRole', 'Role')) return;
        const body = {
            name: val('insUserName'),
            mail: val('insUserMail'),
            hash: val('insUserHash'),
            role: getComboValue('combo_insUserRole')
        };
        if (!body.name || !body.mail || !body.hash) { notify('Name, mail and password are required', 'err'); return; }
        const r = await api('/users', { method: 'POST', body: JSON.stringify(body) });
        if (r.ok) {
            e.target.reset();
            setComboValue('combo_insUserRole', null, '');
            await afterMutation(cargarUsers);
            notify('User created');
        } else notify(r.data?.message || 'Error creating user', 'err');
    });

    document.getElementById('formInsertGuild').addEventListener('submit', async e => {
        e.preventDefault();
        if (!requireCombo('combo_insGuildNumber', 'Number')) return;
        if (!requireCombo('combo_insGuildLetter', 'Letter')) return;
        const number = getComboValue('combo_insGuildNumber');
        const letter = getComboValue('combo_insGuildLetter');
        const modality = getComboValue('combo_insGuildModality');
        const level = getComboValue('combo_insGuildLevel');
        const name = val('insGuildName');
        if (!name) { notify('Name is required', 'err'); return; }
        const body = {
            name,
            number: Number(number),
            letter,
            level: level === '' || level === EMPTY_SENTINEL ? null : level,
            modality: modality === '' || modality === EMPTY_SENTINEL ? null : modality
        };
        const r = await api('/guilds', { method: 'POST', body: JSON.stringify(body) });
        if (r.ok) {
            e.target.reset();
            setComboValue('combo_insGuildNumber', null, '');
            setComboValue('combo_insGuildLetter', null, '');
            setComboValue('combo_insGuildLevel', null, '');
            setComboValue('combo_insGuildModality', null, '');
            await afterMutation(cargarGuilds);
            notify('Guild created');
        } else notify(r.data?.message || 'Error creating guild', 'err');
    });

    document.getElementById('formInsertMentorship').addEventListener('submit', async e => {
        e.preventDefault();
        const userId = getComboValueOrNull('combo_insMentUserId');
        const guildId = getComboValueOrNull('combo_insMentGuildId');
        if (userId == null || guildId == null) { notify('User and Guild are required', 'err'); return; }
        const r = await api('/mentorships', { method: 'POST', body: JSON.stringify({ user_id: userId, guild_id: guildId }) });
        if (r.ok) {
            setComboValue('combo_insMentUserId', null, '');
            setComboValue('combo_insMentGuildId', null, '');
            await afterMutation(cargarMentorships);
            notify('Mentorship created');
        } else notify('Error creating mentorship', 'err');
    });

    document.getElementById('formInsertParty').addEventListener('submit', async e => {
        e.preventDefault();
        const guildId = getComboValueOrNull('combo_insPartyGuildId');
        if (guildId == null) { notify('Guild is required', 'err'); return; }
        const r = await api('/parties', { method: 'POST', body: JSON.stringify({ name: val('insPartyName'), guild_id: guildId }) });
        if (r.ok) {
            e.target.reset();
            setComboValue('combo_insPartyGuildId', null, '');
            await afterMutation(cargarParties);
            notify('Party created');
        } else notify('Error creating party', 'err');
    });

    document.getElementById('formInsertCharacter').addEventListener('submit', async e => {
        e.preventDefault();
        if (!assertNumberMins([{ id: 'insCharExp', min: 0 }])) return;
        if (!requireCombo('combo_insCharJob', 'Job')) return;
        if (!requireCombo('combo_insCharLevel', 'Level')) return;
        if (!requireCombo('combo_insCharUserId', 'User')) return;
        if (!requireCombo('combo_insCharGuildId', 'Guild')) return;
        const job = getComboValue('combo_insCharJob');
        const userId = getComboValueOrNull('combo_insCharUserId');
        const guildId = getComboValueOrNull('combo_insCharGuildId');
        const name = val('insCharName');
        if (!name) { notify('Name is required', 'err'); return; }
        const body = {
            name,
            job,
            level: Number(getComboValue('combo_insCharLevel')),
            exp: Number(val('insCharExp') || 0),
            user_id: userId,
            guild_id: guildId,
            party_id: getComboValueOrNull('combo_insCharPartyId')
        };
        const r = await api('/characters', { method: 'POST', body: JSON.stringify(body) });
        if (r.ok) {
            e.target.reset();
            document.getElementById('insCharExp').value = 0;
            ['combo_insCharJob', 'combo_insCharLevel', 'combo_insCharUserId', 'combo_insCharGuildId', 'combo_insCharPartyId'].forEach(id => setComboValue(id, null, ''));
            await afterMutation(cargarCharacters);
            notify('Character created');
        } else notify('Error creating character', 'err');
    });

    document.getElementById('formInsertSkill').addEventListener('submit', async e => {
        e.preventDefault();
        if (!assertNumberMins([{ id: 'insSkillExpCost', min: 0 }])) return;
        if (!requireCombo('combo_insSkillJob', 'Job')) return;
        if (!requireCombo('combo_insSkillLevelReq', 'Level Req')) return;
        if (!requireCombo('combo_insSkillAoe', 'AOE')) return;
        const name = val('insSkillName');
        const description = val('insSkillDesc');
        if (!name || !description) { notify('Name and Description are required', 'err'); return; }
        const body = {
            name,
            level_req: Number(getComboValue('combo_insSkillLevelReq')),
            job: getComboValue('combo_insSkillJob'),
            aoe: getComboValue('combo_insSkillAoe'),
            exp_cost: Number(val('insSkillExpCost') || 0),
            description
        };
        const r = await api('/skills', { method: 'POST', body: JSON.stringify(body) });
        if (r.ok) {
            e.target.reset();
            document.getElementById('insSkillExpCost').value = 0;
            setComboValue('combo_insSkillJob', null, '');
            setComboValue('combo_insSkillLevelReq', null, '');
            setComboValue('combo_insSkillAoe', null, '');
            await afterMutation(cargarSkills);
            notify('Skill created');
        } else notify('Error creating skill', 'err');
    });

    document.getElementById('formInsertEvent').addEventListener('submit', async e => {
        e.preventDefault();
        if (!requireCombo('combo_insEvCaster', 'Caster')) return;
        if (!requireCombo('combo_insEvSkill', 'Skill')) return;
        if (!requireCombo('combo_insEvGuild', 'Guild')) return;
        if (!requireCombo('combo_insEvStatus', 'Status')) return;
        const comment = val('insEvComment');
        const status = getComboValue('combo_insEvStatus');
        if (!assertCommentForStatus(status, comment)) return;
        const body = {
            caster_character_id: getComboValueOrNull('combo_insEvCaster'),
            skill_id: getComboValueOrNull('combo_insEvSkill'),
            guild_id: getComboValueOrNull('combo_insEvGuild'),
            target_character_id: getComboValueOrNull('combo_insEvTargetChar'),
            target_party_id: getComboValueOrNull('combo_insEvTargetParty'),
            status,
            reviewed_by_user_id: getComboValueOrNull('combo_insEvReviewer'),
            comment: comment === '' ? null : comment
        };
        const r = await api('/events', { method: 'POST', body: JSON.stringify(body) });
        if (r.ok) {
            e.target.reset();
            ['combo_insEvCaster', 'combo_insEvSkill', 'combo_insEvGuild', 'combo_insEvTargetChar', 'combo_insEvTargetParty', 'combo_insEvReviewer', 'combo_insEvStatus']
                .forEach(id => setComboValue(id, null, ''));
            syncInsertEventCommentLabel();
            await afterMutation(cargarEvents);
            notify('Event created');
        } else notify('Error creating event', 'err');
    });

    document.getElementById('formEdit').addEventListener('submit', async e => {
        e.preventDefault();
        if (!editContext) return;
        const body = {};
        for (const f of editContext.fields) {
            if (f.comboId) {
                let v = getComboValue(f.comboId);
                if (f.freeText && isEmptyComboValue(v)) v = combos[f.comboId]?.search.value || '';
                if (f.allowEmpty && isEmptyComboValue(v)) {
                    v = null;
                } else if (isEmptyComboValue(v)) {
                    notify(`${f.label || f.key} is required`, 'err');
                    return;
                } else if (f.type === 'number' || f.numeric) {
                    v = Number(v);
                    const min = f.min != null ? f.min : 1;
                    if (Number.isNaN(v) || v < min) {
                        notify(`${f.label || f.key} must be at least ${min}`, 'err');
                        return;
                    }
                }
                if (f.optionalPassword && isEmptyComboValue(v)) continue;
                body[f.key] = v;
            } else {
                const el = document.getElementById(`edit_${f.key}`);
                if (!el) continue;
                let v = el.value.trim();
                if (f.optionalPassword && v === '') continue;
                if (f.type === 'number') {
                    const min = f.min != null ? f.min : (/exp/i.test(f.key) ? 0 : 1);
                    if (v === '' || Number(v) < min) {
                        notify(`${f.label || f.key} must be at least ${min}`, 'err');
                        return;
                    }
                    v = Number(v);
                } else if (!f.allowEmpty && v === '') {
                    notify(`${f.label || f.key} is required`, 'err');
                    return;
                } else if (f.allowEmpty && v === '') {
                    v = null;
                }
                body[f.key] = v;
            }
        }
        if (editContext.kind === 'event') {
            if (!assertCommentForStatus(body.status, body.comment)) return;
            if (body.comment === '') body.comment = null;
        }
        const r = await api(editContext.path, { method: 'PUT', body: JSON.stringify(body) });
        if (r.ok) {
            const reload = editContext.reload;
            cerrarModal();
            await afterMutation(reload);
            notify('Updated');
        } else notify(r.data?.message || 'Error updating', 'err');
    });
}

function acciones(editFn, deleteFn) {
    return `<div class="action-buttons">
        <button type="button" class="btn-small btn-warning" onclick="${editFn}">Edit</button>
        <button type="button" class="btn-small btn-danger" onclick="${deleteFn}">Delete</button>
    </div>`;
}

async function cargarUsers() {
    const r = await api('/users');
    usersCache = Array.isArray(r.data) ? r.data : [];
    document.getElementById('usersBody').innerHTML = usersCache.map(u => `
        <tr>
            <td>${u.id}</td><td>${u.name || ''}</td><td>${u.mail || ''}</td>
            <td style="max-width:220px;word-break:break-all;font-size:12px;">${u.hash || ''}</td>
            <td>${u.role || ''}</td>
            <td>${acciones(`editarUser(${u.id})`, `borrar('/users/${u.id}', () => afterMutation(cargarUsers))`)}</td>
        </tr>`).join('') || '<tr><td colspan="6">No data</td></tr>';
    reapplyFilters();
}

window.editarUser = async function (id) {
    const r = await api(`/users/${id}`);
    if (!r.ok) return notify('Not found', 'err');
    abrirEdit('Edit User', `/users/${id}`, cargarUsers, [
        { key: 'name', label: 'Name', value: r.data.name },
        { key: 'mail', label: 'Mail', value: r.data.mail },
        { key: 'hash', label: 'New password (leave empty to keep)', value: '', type: 'password', optionalPassword: true },
        { key: 'role', label: 'Role', combo: true, options: ROLES.map(x => ({ value: x, label: x })), value: r.data.role }
    ]);
};

async function cargarGuilds() {
    const r = await api('/guilds');
    guildsCache = Array.isArray(r.data) ? r.data : [];
    document.getElementById('guildsBody').innerHTML = guildsCache.map(g => `
        <tr>
            <td>${g.id}</td>
            <td>${g.name || ''}</td>
            <td>${g.number ?? ''}</td>
            <td>${g.letter || ''}</td>
            <td>${cellOrNull(g.level)}</td>
            <td>${cellOrNull(g.modality)}</td>
            <td>${acciones(`editarGuild(${g.id})`, `borrar('/guilds/${g.id}', () => afterMutation(cargarGuilds))`)}</td>
        </tr>`).join('') || '<tr><td colspan="7">No data</td></tr>';
    reapplyFilters();
}

window.editarGuild = async function (id) {
    const r = await api(`/guilds/${id}`);
    if (!r.ok) return notify('Not found', 'err');
    abrirEdit('Edit Guild', `/guilds/${id}`, cargarGuilds, [
        { key: 'name', label: 'Name', value: r.data.name },
        { key: 'number', label: 'Number', combo: true, numeric: true,
            options: CURSOS.map(n => ({ value: Number(n), label: n })), value: r.data.number },
        { key: 'letter', label: 'Letter', combo: true,
            options: LETTERS.map(l => ({ value: l, label: l })), value: r.data.letter },
        { key: 'level', label: 'Level', combo: true, allowEmpty: true, emptyLabel: '--Empty--',
            options: LEVELS.map(l => ({ value: l, label: l })), value: r.data.level || '' },
        { key: 'modality', label: 'Modality', combo: true, allowEmpty: true, emptyLabel: '--Empty--',
            options: MODALITIES.map(m => ({ value: m, label: m })), value: r.data.modality || '' }
    ]);
};

async function cargarMentorships() {
    const r = await api('/mentorships');
    const rows = Array.isArray(r.data) ? r.data : [];
    document.getElementById('mentorshipsBody').innerHTML = rows.map(m => {
        const u = userById(m.user_id);
        const g = guildById(m.guild_id);
        return `<tr>
            <td>${idName(m.user_id, u?.name)}</td>
            <td>${g ? guildLabel(g) : idName(m.guild_id, '?')}</td>
            <td>${acciones(`editarMentorship(${m.user_id}, ${m.guild_id})`, `borrar('/mentorships/${m.user_id}/${m.guild_id}', () => afterMutation(cargarMentorships))`)}</td>
        </tr>`;
    }).join('') || '<tr><td colspan="3">No data</td></tr>';
    reapplyFilters();
}

window.editarMentorship = function (userId, guildId) {
    abrirEdit('Edit Mentorship', `/mentorships/${userId}/${guildId}`, cargarMentorships, [
        { key: 'user_id', label: 'User (Teacher)', combo: true, numeric: true,
            options: teacherOpts(), value: userId },
        { key: 'guild_id', label: 'Guild', combo: true, numeric: true,
            options: guildOpts(), value: guildId }
    ]);
};

async function cargarParties() {
    const r = await api('/parties');
    partiesCache = Array.isArray(r.data) ? r.data : [];
    document.getElementById('partiesBody').innerHTML = partiesCache.map(p => {
        const g = guildById(p.guild_id);
        return `<tr>
            <td>${p.id}</td><td>${p.name || ''}</td>
            <td>${g ? guildLabel(g) : idName(p.guild_id, '?')}</td>
            <td>${acciones(`editarParty(${p.id})`, `borrar('/parties/${p.id}', () => afterMutation(cargarParties))`)}</td>
        </tr>`;
    }).join('') || '<tr><td colspan="4">No data</td></tr>';
    reapplyFilters();
}

window.editarParty = async function (id) {
    const r = await api(`/parties/${id}`);
    if (!r.ok) return notify('Not found', 'err');
    abrirEdit('Edit Party', `/parties/${id}`, cargarParties, [
        { key: 'name', label: 'Name', value: r.data.name },
        { key: 'guild_id', label: 'Guild', combo: true, numeric: true,
            options: guildOpts(), value: r.data.guild_id }
    ]);
};

async function cargarCharacters() {
    const r = await api('/characters');
    charactersCache = Array.isArray(r.data) ? r.data : [];
    document.getElementById('charactersBody').innerHTML = charactersCache.map(c => {
        const u = userById(c.user_id);
        const g = guildById(c.guild_id);
        const p = partyById(c.party_id);
        return `<tr>
            <td>${c.id}</td><td>${c.name || ''}</td><td>${c.job || ''}</td><td>${c.level ?? ''}</td>
            <td>${c.exp ?? ''}</td>
            <td>${idName(c.user_id, u?.name)}</td>
            <td>${g ? guildLabel(g) : idName(c.guild_id, '?')}</td>
            <td>${cellOrNull(c.party_id, c.party_id != null ? idName(c.party_id, p?.name) : null)}</td>
            <td>${acciones(`editarCharacter(${c.id})`, `borrar('/characters/${c.id}', () => afterMutation(cargarCharacters))`)}</td>
        </tr>`;
    }).join('') || '<tr><td colspan="9">No data</td></tr>';
    reapplyFilters();
}

window.editarCharacter = async function (id) {
    const r = await api(`/characters/${id}`);
    if (!r.ok) return notify('Not found', 'err');
    abrirEdit('Edit Character', `/characters/${id}`, cargarCharacters, [
        { key: 'name', label: 'Name', value: r.data.name },
        { key: 'job', label: 'Job', combo: true, options: jobOpts(), value: r.data.job },
        { key: 'level', label: 'Level', combo: true, numeric: true,
            options: CURSOS.map(n => ({ value: Number(n), label: n })), value: r.data.level },
        { key: 'exp', label: 'Exp', value: r.data.exp, type: 'number', min: 0 },
        { key: 'user_id', label: 'User', combo: true, numeric: true, options: userOpts(), value: r.data.user_id },
        {
            key: 'guild_id', label: 'Guild', combo: true, numeric: true, options: guildOpts(), value: r.data.guild_id,
            onChange: (guildId) => {
                const partyField = editContext?.fields?.find(f => f.key === 'party_id');
                if (!partyField?.comboId) return;
                const opts = partyOptsForGuild(guildId);
                setComboOptions(partyField.comboId, opts);
                const cur = getComboValueOrNull(partyField.comboId);
                if (cur != null && !opts.some(o => Number(o.value) === Number(cur))) {
                    setComboValue(partyField.comboId, null, '');
                }
            }
        },
        {
            key: 'party_id', label: 'Party', combo: true, numeric: true, allowEmpty: true, emptyLabel: '--Empty--',
            options: partyOptsForGuild(r.data.guild_id), value: r.data.party_id
        }
    ]);
};

async function cargarSkills() {
    const r = await api('/skills');
    skillsCache = Array.isArray(r.data) ? r.data : [];
    document.getElementById('skillsBody').innerHTML = skillsCache.map(s => `
        <tr>
            <td>${s.id}</td><td>${s.name || ''}</td><td>${s.level_req ?? ''}</td><td>${s.job || ''}</td>
            <td>${s.aoe || ''}</td><td>${s.exp_cost ?? ''}</td><td>${s.description || ''}</td>
            <td>${acciones(`editarSkill(${s.id})`, `borrar('/skills/${s.id}', () => afterMutation(cargarSkills))`)}</td>
        </tr>`).join('') || '<tr><td colspan="8">No data</td></tr>';
    reapplyFilters();
}

window.editarSkill = async function (id) {
    const r = await api(`/skills/${id}`);
    if (!r.ok) return notify('Not found', 'err');
    abrirEdit('Edit Skill', `/skills/${id}`, cargarSkills, [
        { key: 'name', label: 'Name', value: r.data.name },
        { key: 'level_req', label: 'Level Req', combo: true, numeric: true,
            options: CURSOS.map(n => ({ value: Number(n), label: n })), value: r.data.level_req },
        { key: 'job', label: 'Job', combo: true, options: jobOpts(), value: r.data.job },
        { key: 'aoe', label: 'AOE', combo: true, options: AOES.map(a => ({ value: a, label: a })), value: r.data.aoe },
        { key: 'exp_cost', label: 'Exp Cost', value: r.data.exp_cost, type: 'number', min: 0 },
        { key: 'description', label: 'Description', value: r.data.description || '' }
    ]);
};

async function cargarEvents() {
    const r = await api('/events');
    const rows = Array.isArray(r.data) ? r.data : [];
    document.getElementById('eventsBody').innerHTML = rows.map(ev => {
        const caster = characterById(ev.caster_character_id);
        const skill = skillById(ev.skill_id);
        const guild = guildById(ev.guild_id);
        const tChar = characterById(ev.target_character_id);
        const tParty = partyById(ev.target_party_id);
        const reviewer = userById(ev.reviewed_by_user_id);
        return `<tr>
            <td>${ev.id}</td>
            <td>${idName(ev.caster_character_id, caster?.name)}</td>
            <td>${idName(ev.skill_id, skill?.name)}</td>
            <td>${guild ? guildLabel(guild) : idName(ev.guild_id, '?')}</td>
            <td>${cellOrNull(ev.target_character_id, ev.target_character_id != null ? idName(ev.target_character_id, tChar?.name) : null)}</td>
            <td>${cellOrNull(ev.target_party_id, ev.target_party_id != null ? idName(ev.target_party_id, tParty?.name) : null)}</td>
            <td>${ev.status || ''}</td>
            <td>${cellOrNull(ev.reviewed_by_user_id, ev.reviewed_by_user_id != null ? idName(ev.reviewed_by_user_id, reviewer?.name) : null)}</td>
            <td>${ev.created_at || ''}</td>
            <td>${cellOrNull(ev.comment)}</td>
            <td>${acciones(`editarEvent(${ev.id})`, `borrar('/events/${ev.id}', () => afterMutation(cargarEvents))`)}</td>
        </tr>`;
    }).join('') || '<tr><td colspan="11">No data</td></tr>';
    reapplyFilters();
}

window.editarEvent = async function (id) {
    const r = await api(`/events/${id}`);
    if (!r.ok) return notify('Not found', 'err');
    abrirEdit('Edit Event', `/events/${id}`, cargarEvents, [
        { key: 'caster_character_id', label: 'Caster', combo: true, numeric: true,
            options: characterOpts(), value: r.data.caster_character_id },
        { key: 'skill_id', label: 'Skill', combo: true, numeric: true,
            options: skillOpts(), value: r.data.skill_id },
        { key: 'guild_id', label: 'Guild', combo: true, numeric: true,
            options: guildOpts(), value: r.data.guild_id },
        { key: 'target_character_id', label: 'Target Character', combo: true, numeric: true, allowEmpty: true, emptyLabel: '--Empty--',
            options: characterOpts(), value: r.data.target_character_id },
        { key: 'target_party_id', label: 'Target Party', combo: true, numeric: true, allowEmpty: true, emptyLabel: '--Empty--',
            options: partyOpts(), value: r.data.target_party_id },
        {
            key: 'status', label: 'Status', combo: true, options: STATUSES.map(s => ({ value: s, label: s })), value: r.data.status,
            onChange: (status) => {
                const commentField = editContext?.fields?.find(f => f.key === 'comment');
                const wrap = document.getElementById('edit_comment')?.closest('.form-group');
                const commentLab = wrap?.querySelector('label');
                const required = commentRequiredForStatus(status);
                setCommentLabelRequired(commentLab, required);
                if (commentField) commentField.allowEmpty = !required;
            }
        },
        { key: 'reviewed_by_user_id', label: 'Reviewed By (Teacher)', combo: true, numeric: true, allowEmpty: true, emptyLabel: '--Empty--',
            options: teacherOpts(), value: r.data.reviewed_by_user_id },
        {
            key: 'comment',
            label: 'Comment',
            value: r.data.comment || '',
            allowEmpty: !commentRequiredForStatus(r.data.status),
            hint: 'Required when Status is APPROVED or REJECTED'
        }
    ], 'event');
};

function fieldLabelHtml(f) {
    const required = !f.allowEmpty && !f.optionalPassword;
    return required
        ? `${f.label}<span class="req">*</span>`
        : f.label;
}

function abrirEdit(title, path, reload, fields, kind = null) {
    editContext = { path, reload, fields: [], kind };
    document.getElementById('modalTitle').textContent = title;
    const box = document.getElementById('editFields');
    box.innerHTML = '';

    fields.forEach(f => {
        if (f.combo) {
            const wrap = document.createElement('div');
            wrap.className = 'form-group';
            const lab = document.createElement('label');
            lab.innerHTML = fieldLabelHtml(f);
            const host = document.createElement('div');
            const comboId = `combo_edit_${f.key}`;
            host.id = comboId;
            wrap.appendChild(lab);
            wrap.appendChild(host);
            if (f.hint) {
                const hint = document.createElement('small');
                hint.className = 'field-hint';
                hint.textContent = f.hint;
                wrap.appendChild(hint);
            }
            box.appendChild(wrap);
            createCombo(comboId, {
                allowEmpty: !!f.allowEmpty,
                emptyLabel: f.emptyLabel || '--Empty--',
                freeText: !!f.freeText
            });
            setComboOptions(comboId, f.options || []);
            if (f.value != null && f.value !== '') {
                const opt = (f.options || []).find(o => String(o.value) === String(f.value));
                setComboValue(comboId, f.value, opt ? opt.label : String(f.value));
            } else if (f.allowEmpty) {
                setComboValue(comboId, EMPTY_SENTINEL, f.emptyLabel || '--Empty--');
            } else {
                setComboValue(comboId, null, '');
            }
            editContext.fields.push({ ...f, comboId });
            if (typeof f.onChange === 'function') {
                combos[comboId].onChange = f.onChange;
            }
        } else {
            const t = f.type === 'password' ? 'password' : (f.type === 'number' ? 'number' : 'text');
            const min = f.type === 'number' ? (f.min != null ? f.min : (/exp/i.test(f.key) ? 0 : 1)) : null;
            const minAttr = min != null ? ` min="${min}"` : '';
            const wrap = document.createElement('div');
            wrap.className = 'form-group';
            wrap.innerHTML = `<label>${fieldLabelHtml(f)}</label>
                <input type="${t}" id="edit_${f.key}" value="${f.value ?? ''}"${minAttr}>`;
            if (f.hint) {
                const hint = document.createElement('small');
                hint.className = 'field-hint';
                hint.textContent = f.hint;
                wrap.appendChild(hint);
            }
            box.appendChild(wrap);
            editContext.fields.push(f);
        }
    });

    document.getElementById('modalEdit').style.display = 'block';
}

window.borrar = async function (path, reloadFn) {
    if (!confirm('Are you sure you want to delete this?')) return;
    const r = await api(path, { method: 'DELETE' });
    if (r.ok || r.status === 204) {
        await reloadFn();
        notify('Deleted');
    } else notify('Error deleting', 'err');
};
