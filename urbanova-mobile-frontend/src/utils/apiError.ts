import { API_BASE_URL } from '@services/api';

export const getApiErrorMessage = (error: any, fallback: string) => {
  const serverMessage = error?.response?.data?.error?.message || error?.response?.data?.message;
  if (serverMessage) {
    return serverMessage;
  }
  if (error?.response?.status) {
    return `${fallback} Server returned ${error.response.status}.`;
  }
  if (error?.code === 'ECONNABORTED') {
    return `${fallback} Request timed out. API: ${API_BASE_URL}`;
  }
  if (error?.message === 'Network Error' || !error?.response) {
    return `${fallback} Network Error. API: ${API_BASE_URL}`;
  }
  return error?.message || fallback;
};
