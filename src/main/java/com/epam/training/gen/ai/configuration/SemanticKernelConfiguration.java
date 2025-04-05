package com.epam.training.gen.ai.configuration;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.epam.training.gen.ai.plugin.AgeCalculatorPlugin;
import com.epam.training.gen.ai.plugin.CurrencyConverterPlugin;
import com.epam.training.gen.ai.plugin.LightsPlugin;
import com.epam.training.gen.ai.plugin.SearchUrlPlugin;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up Semantic Kernel components.
 * <p>
 * This configuration provides several beans necessary for the interaction with Azure OpenAI
 * services and the creation of kernel plugins. It defines beans for chat completion services,
 * kernel plugins, kernel instance, invocation context, and prompt execution settings.
 */
@Configuration
public class SemanticKernelConfiguration {

  /**
   * Creates a {@link ChatCompletionService} bean for handling chat completions using Azure OpenAI.
   *
   * @param deploymentOrModelName the Azure OpenAI deployment or model name
   * @param openAIAsyncClient     the {@link OpenAIAsyncClient} to communicate with Azure OpenAI
   * @return an instance of {@link ChatCompletionService}
   */
  @Bean(name = "chatCompletionService")
  public ChatCompletionService chatCompletionService(
      @Value("${client.azureopenai.deployment-name}") String deploymentOrModelName,
      OpenAIAsyncClient openAIAsyncClient) {
    return OpenAIChatCompletion.builder()
        .withModelId(deploymentOrModelName)
        .withOpenAIAsyncClient(openAIAsyncClient)
        .build();
  }

  /**
   * Creates an {@link InvocationContext} bean with default prompt execution settings.
   *
   * @return an instance of {@link InvocationContext}
   */
  @Bean
  public InvocationContext invocationContext() {
    return InvocationContext.builder()
        .withPromptExecutionSettings(PromptExecutionSettings.builder()
            .withTemperature(0.5)
            .build())
        .build();
  }

  @Bean
  public Kernel semanticKernel(ChatCompletionService chatCompletionService) {
    KernelPlugin lightPlugin = KernelPluginFactory.createFromObject(new LightsPlugin(),
        "LightsPlugin");
    KernelPlugin ageCalculatorPlugin = KernelPluginFactory.createFromObject(
        new AgeCalculatorPlugin(),
        "AgeCalculatorPlugin");
    KernelPlugin currencyConverterPlugin = KernelPluginFactory.createFromObject(
        new CurrencyConverterPlugin(),
        "CurrencyConverterPlugin");
    KernelPlugin searchUrlPlugin = KernelPluginFactory.createFromObject(
        new SearchUrlPlugin(),
        "SearchUrlPlugin");

    return Kernel.builder()
        .withAIService(ChatCompletionService.class, chatCompletionService)
        .withPlugin(lightPlugin)
        .withPlugin(ageCalculatorPlugin)
        .withPlugin(currencyConverterPlugin)
        .withPlugin(searchUrlPlugin)
        .build();
  }
}


