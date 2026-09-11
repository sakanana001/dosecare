// Site-wide constants. Keep in sync with astro.config.mjs `base`.

export const SITE_BASE = '/PicoHRT/';
export const SITE_ORIGIN = 'https://sakanana001.github.io';

/** Build an internal URL that respects the GitHub Pages base path. */
export function siteUrl(path = '/'): string {
  const cleanPath = path.startsWith('/') ? path.slice(1) : path;
  if (!cleanPath) return SITE_BASE;
  return `${SITE_BASE}${cleanPath}`;
}
