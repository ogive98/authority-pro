import type { Language } from './languages';
import { ar } from './catalogs/ar';
import { en } from './catalogs/en';
import { fr } from './catalogs/fr';
import { it } from './catalogs/it';

type ReplaceLiterals<T> = {
  [K in keyof T]: T[K] extends string ? string : ReplaceLiterals<T[K]>;
};

type Leaves<T> = {
  [K in keyof T & string]: T[K] extends string ? K : `${K}.${Leaves<T[K]>}`;
}[keyof T & string];

export type Dictionary = ReplaceLiterals<typeof fr>;
export type TranslationKey = Leaves<typeof fr>;

export const DICTIONARIES: Record<Language, Dictionary> = { fr, en, it, ar };
