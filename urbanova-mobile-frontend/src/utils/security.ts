export const maskCard = (brand: string, last4: string) => `${brand.toUpperCase()} **** **** **** ${last4}`;

export const sanitizeCardNumber = (value: string) => value.replace(/\D/g, '');

export const formatCardNumberForInput = (value: string) => {
  const digits = sanitizeCardNumber(value).slice(0, 19);
  return digits.replace(/(\d{4})(?=\d)/g, '$1 ').trim();
};

export const sanitizeExpiryInput = (value: string) => value.replace(/\D/g, '').slice(0, 4);

export const parseExpiry = (value: string): { month: number; year: number } | null => {
  const digits = sanitizeExpiryInput(value);
  if (digits.length !== 4) {
    return null;
  }

  const month = Number(digits.slice(0, 2));
  const year = Number(`20${digits.slice(2, 4)}`);
  if (!Number.isInteger(month) || month < 1 || month > 12) {
    return null;
  }
  if (!Number.isInteger(year) || year < 2000) {
    return null;
  }

  return { month, year };
};

export const validatePasswordStrength = (password: string) => {
  if (password.length < 8) {
    return 'Password must be at least 8 characters.';
  }
  if (!/[A-Z]/.test(password) || !/[a-z]/.test(password) || !/\d/.test(password)) {
    return 'Use at least one uppercase letter, one lowercase letter, and one number.';
  }
  return '';
};
