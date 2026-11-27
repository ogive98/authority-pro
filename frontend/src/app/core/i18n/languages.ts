export type Language = 'fr' | 'en' | 'it' | 'ar';
export type Direction = 'ltr' | 'rtl';

export interface LanguageOption {
  readonly code: Language;
  readonly label: string;
  readonly dir: Direction;
  readonly locale: string;
}

export const DEFAULT_LANGUAGE: Language = 'fr';

export const LANGUAGES: readonly LanguageOption[] = [
  { code: 'fr', label: 'Français', dir: 'ltr', locale: 'fr-FR' },
  { code: 'en', label: 'English', dir: 'ltr', locale: 'en-GB' },
  { code: 'it', label: 'Italiano', dir: 'ltr', locale: 'it-IT' },
  { code: 'ar', label: 'العربية', dir: 'rtl', locale: 'ar' },
];

export function languageOption(language: Language): LanguageOption {
  const option = LANGUAGES.find((candidate) => candidate.code === language);
  if (!option) throw new Error(`Unknown language: ${language}`);
  return option;
}

export function isLanguage(value: unknown): value is Language {
  return typeof value === 'string' && LANGUAGES.some((option) => option.code === value);
}
