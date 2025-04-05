package com.epam.training.gen.ai.plugin;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import java.util.HashMap;
import java.util.Map;

public class CurrencyConverterPlugin {

  private final Map<String, Double> exchangeRates = new HashMap<>();

  public CurrencyConverterPlugin() {
    exchangeRates.put("USD", 1.0); // Set base currency (USD)
    exchangeRates.put("EUR", 0.93);
    exchangeRates.put("GBP", 0.81);
    exchangeRates.put("JPY", 134.49);
    exchangeRates.put("ETB", 150.49);
  }

  @DefineKernelFunction(name = "covert_currency", description = "Convert a given amount from given currency to another")
  public double convert(
      @KernelFunctionParameter(name = "amount", description = "amount") double amount,
      @KernelFunctionParameter(name = "fromCurrency", description = "from currency") String fromCurrency,
      @KernelFunctionParameter(name = "toCurrency", description = "to currency") String toCurrency
  ) {
    System.out.printf(
        "Using CurrencyConverterPlugin to convert currency: amount %s, %s to %s\n",
        amount, fromCurrency, toCurrency);

    if (!exchangeRates.containsKey(fromCurrency) || !exchangeRates.containsKey(toCurrency)) {
      throw new IllegalArgumentException("[CurrencyConverterPlugin] Unsupported currency code.");
    }

    double rateFromBase = exchangeRates.get(fromCurrency);
    double rateToBase = exchangeRates.get(toCurrency);

    return amount * (rateToBase / rateFromBase);
  }
}
