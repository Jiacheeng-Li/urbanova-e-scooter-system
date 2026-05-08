import { AppliedDiscount, DiscountEligibility, PriceQuote } from '@services/api';

const PROMOTION_LABELS: Record<string, string> = {
  NEW_RIDER: 'New rider discount',
  FIRST_RIDE: 'New rider discount',
  YOUTH: 'Youth discount',
  STUDENT: 'Youth discount',
  SENIOR: 'Senior discount',
  LOYALTY: 'Frequent rider discount',
  FREQUENT_USER: 'Frequent rider discount',
  AGE_BASED: 'Age-based discount',
};

const ALLOWED_PROMOTION_PATTERNS = ['NEW', 'FIRST', 'YOUTH', 'STUDENT', 'SENIOR', 'LOYAL', 'FREQUENT'];

export const isUserFacingPromotionType = (type?: string) => {
  if (!type) {
    return false;
  }
  const normalized = type.toUpperCase();
  return ALLOWED_PROMOTION_PATTERNS.some((pattern) => normalized.includes(pattern));
};

export const getPromotionLabel = (type?: string) => {
  if (!type) {
    return 'Promotion applied';
  }
  const normalized = type.toUpperCase();
  const direct = PROMOTION_LABELS[normalized];
  if (direct) {
    return direct;
  }
  if (normalized.includes('SENIOR') || normalized.includes('65')) {
    return 'Senior discount';
  }
  if (normalized.includes('YOUTH') || normalized.includes('STUDENT') || normalized.includes('22')) {
    return 'Youth discount';
  }
  if (normalized.includes('NEW') || normalized.includes('FIRST')) {
    return 'New rider discount';
  }
  if (normalized.includes('LOYAL') || normalized.includes('FREQUENT')) {
    return 'Frequent rider discount';
  }
  if (!isUserFacingPromotionType(normalized)) {
    return 'Promotion applied';
  }
  return type
    .toLowerCase()
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
};

export const getBestDiscount = (discounts?: AppliedDiscount[]) =>
  (discounts ?? []).reduce<AppliedDiscount | null>((best, current) => {
    if (!best || Number(current.amount || 0) > Number(best.amount || 0)) {
      return current;
    }
    return best;
  }, null);

export const getQuoteDiscountSummary = (quote?: PriceQuote | null) => {
  if (!quote || !quote.appliedDiscounts?.length) {
    return null;
  }
  const basePrice = Number(quote.basePrice || 0);
  const finalPrice = Number(quote.finalPrice || 0);
  const userFacingDiscounts = quote.appliedDiscounts.filter((discount) => isUserFacingPromotionType(discount.type));
  if (userFacingDiscounts.length === 0) {
    return null;
  }
  const best = getBestDiscount(userFacingDiscounts);
  const saved = Math.max(0, basePrice - finalPrice);
  const percent = basePrice > 0 ? Math.round((saved / basePrice) * 100) : 0;
  return {
    label: getPromotionLabel(best?.type),
    percent,
    saved,
  };
};

export const getEligibilityHeadline = (eligibility?: DiscountEligibility | null) => {
  const firstEligibleType = eligibility?.eligibleTypes?.find(isUserFacingPromotionType);
  if (isUserFacingPromotionType(firstEligibleType)) {
    return getPromotionLabel(firstEligibleType);
  }
  if (eligibility?.age && eligibility.age > 65) {
    return 'Senior discount';
  }
  if (eligibility?.age && eligibility.age < 22) {
    return 'Youth discount';
  }
  return 'No active promotion yet';
};
