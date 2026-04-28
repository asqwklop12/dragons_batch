export const ITEM_CATEGORIES = [
  { value: '100', label: '식량작물', emoji: '🌾' },
  { value: '200', label: '채소류', emoji: '🥬' },
  { value: '300', label: '특용작물', emoji: '🌿' },
  { value: '400', label: '과일류', emoji: '🍎' },
  { value: '500', label: '축산물', emoji: '🥩' },
  { value: '600', label: '수산물', emoji: '🐟' },
] as const;

export type ItemCategoryCode = (typeof ITEM_CATEGORIES)[number]['value'];

export const DEFAULT_ITEM_CATEGORY: ItemCategoryCode = ITEM_CATEGORIES[0]?.value ?? '100';
