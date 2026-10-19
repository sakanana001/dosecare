// i18n helper: load the right JSON dictionary based on Astro.url.pathname,
// resolve a path to a sibling path in another locale.

import en from './en.json';
import zhHans from './zh-Hans.json';
import { siteUrl } from '../config';

export type Locale = 'en' | 'zh-Hans';
export const DEFAULT_LOCALE: Locale = 'en';
export const LOCALES: Locale[] = ['en', 'zh-Hans'];

const dictionaries = {
  en,
  'zh-Hans': zhHans,
} as const;

export type Dictionary = typeof en;

export function getLocaleFromPath(pathname: string): Locale {
  // Strip the GitHub Pages base prefix before checking the locale segment
  // so /DoseCare/zh-Hans/privacy/ reads as zh-Hans.
  const baseStripped = pathname.replace(/^\/DoseCare\/?/, '/');
  const seg = baseStripped.split('/').filter(Boolean)[0]?.toLowerCase();
  if (seg === 'zh-hans' || seg === 'zh-cn' || seg === 'zh') {
    return 'zh-Hans';
  }
  return DEFAULT_LOCALE;
}

export function getDictionary(locale: Locale): Dictionary {
  return dictionaries[locale] ?? dictionaries[DEFAULT_LOCALE];
}

// Path with locale prefix AND GitHub Pages base. Default locale drops the
// locale prefix but keeps the base. All paths are absolute to the site
// origin (i.e., start with the base).
export function localizedPath(locale: Locale, path: string): string {
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  const normalized = cleanPath === '/' ? '' : cleanPath;
  if (locale === DEFAULT_LOCALE) {
    return siteUrl(normalized || '/');
  }
  if (normalized === '' || normalized === '/') {
    return siteUrl(`${locale}/`);
  }
  return siteUrl(`${locale}${normalized}`);
}

// Strip BOTH the GitHub Pages base and the locale prefix from a path so
// the language switcher can swap the locale while keeping the rest of
// the URL identical.
export function stripLocale(pathname: string): string {
  // Drop the base prefix.
  const baseStripped = pathname.replace(/^\/DoseCare\/?/, '/');
  const seg = baseStripped.split('/').filter(Boolean)[0]?.toLowerCase();
  if (seg === 'zh-hans' || seg === 'zh-cn' || seg === 'zh') {
    const rest = baseStripped.split('/').filter(Boolean).slice(1).join('/');
    return rest ? `/${rest}` : '/';
  }
  return baseStripped || '/';
}

// Build a sister URL: same page, different locale.
export function sisterPath(currentPath: string, target: Locale): string {
  const stripped = stripLocale(currentPath);
  return localizedPath(target, stripped);
}

export const LOCALE_LABEL: Record<Locale, string> = {
  en: 'English',
  'zh-Hans': '简体中文',
};

export const LOCALE_SHORT: Record<Locale, string> = {
  en: 'EN',
  'zh-Hans': '中',
};
