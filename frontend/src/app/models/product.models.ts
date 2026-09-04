export interface Product {
  id: number;
  productSku: string;
  productName: string;
  category: string;
  purchaseDate: string;
  unitPrice: number;
  quantity: number;
  stockAgeDays: number;
}

export interface ProductSummary {
  totalProducts: number;
  totalInventoryValue: number;
  averageStockAgeDays: number;
}

export interface ImportResponse {
  message: string;
  importedCount: number;
}

export interface ValidationError {
  row?: number;
  field?: string;
  message: string;
  suggestion?: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  message: string;
  errors: ValidationError[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
