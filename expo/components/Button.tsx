import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children: React.ReactNode;
  className?: string;
}

export const Button: React.FC<ButtonProps> = ({ 
  children, 
  className = '', 
  ...props 
}) => {
  const baseClasses = "flex items-center justify-center px-4 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-md transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transform active:scale-95";
  const combinedClasses = className ? `${baseClasses} ${className}` : baseClasses;

  return (
    <button 
      className={combinedClasses}
      {...props}
    >
      {children}
    </button>
  );
};

export default Button;
