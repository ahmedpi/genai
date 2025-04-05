package com.epam.training.gen.ai.plugin;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SearchUrlPlugin {

  private static final String BASE_URL = "https://en.wikipedia.org/wiki/Special:Search";

  @DefineKernelFunction(name = "generate_wikipedia_url", description = "Generates a URL for searching the given query on Wikipedia")
  public String getWikipediaSearchUrl(
      @KernelFunctionParameter(name = "query", description = "query to be searched in Wikipedia") String query
  ) {
    System.out.println(
        "Using SearchUrlPlugin to generate a URL for searching query " + query + " on Wikipedia");

    String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
    return BASE_URL + "/?search=" + encodedQuery;
  }

}
