(() => {
	const STORAGE_KEY = 'techpulse-theme';
	const THEME_DARK = 'dark';
	const THEME_LIGHT = 'light';

	function safeGetTheme() {
		try {
			const value = localStorage.getItem(STORAGE_KEY);
			if (value === THEME_DARK || value === THEME_LIGHT) {
				return value;
			}
		} catch (error) {
			// Ignore storage access issues and fall back to default.
		}
		return THEME_DARK;
	}

	function safeStoreTheme(theme) {
		try {
			localStorage.setItem(STORAGE_KEY, theme);
		} catch (error) {
			// Ignore storage access issues.
		}
	}

	function updateToggleText(theme) {
		const nextTheme = theme === THEME_DARK ? THEME_LIGHT : THEME_DARK;
		document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
			button.textContent = `Theme: ${theme === THEME_DARK ? 'Dark' : 'Light'}`;
			button.setAttribute('aria-label', `Switch to ${nextTheme} theme`);
		});
	}

	function applyTheme(theme) {
		document.documentElement.setAttribute('data-theme', theme);
		updateToggleText(theme);
	}

	function toggleTheme() {
		const currentTheme = document.documentElement.getAttribute('data-theme') || THEME_DARK;
		const nextTheme = currentTheme === THEME_DARK ? THEME_LIGHT : THEME_DARK;
		applyTheme(nextTheme);
		safeStoreTheme(nextTheme);
	}

	function initThemeToggle() {
		applyTheme(safeGetTheme());
		document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
			button.addEventListener('click', toggleTheme);
		});
	}

	document.addEventListener('DOMContentLoaded', initThemeToggle);
})();

