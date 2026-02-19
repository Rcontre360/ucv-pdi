import React from 'react';

interface SliderProps {
  id: string;
  label: string;
  min: number;
  max: number;
  step?: number;
  value: number;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  disabled?: boolean;
  className?: string;
}

const Slider: React.FC<SliderProps> = ({
  id,
  label,
  min,
  max,
  step = 0.01,
  value,
  onChange,
  disabled = false,
  className = '',
}) => {
  return (
    <div className={`form-group row ${className}`}>
      <label className="col-form-label-md" htmlFor={id}>
        {label}
      </label>
      <input
        className="form-control-range"
        type="range"
        id={id}
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={onChange}
        disabled={disabled}
      />
    </div>
  );
};

export default Slider;
