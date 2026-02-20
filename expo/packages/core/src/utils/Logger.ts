import { Request, Response, NextFunction } from 'express';

export const requestLogger = (req: Request, res: Response, next: NextFunction) => {
  const start = Date.now();
  const { method, url } = req;

  res.on('finish', () => {
    const duration = Date.now() - start;
    const { statusCode } = res;
    console.log(`[${new Date().toISOString()}] ${method} ${url} ${statusCode} - ${duration}ms`);
  });

  next();
};

export const logger = {
  info: (message: string) => console.log(`[INFO] ${new Date().toISOString()} - ${message}`),
  error: (message: string, error?: any) => console.error(`[ERROR] ${new Date().toISOString()} - ${message}`, error),
  warn: (message: string) => console.warn(`[WARN] ${new Date().toISOString()} - ${message}`),
};
