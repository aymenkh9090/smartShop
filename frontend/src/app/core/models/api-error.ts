/** Format des erreurs renvoyees par le backend (GlobalExceptionHandler). */
export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  message: string;
  path: string;
  errors: ApiFieldError[];
}
