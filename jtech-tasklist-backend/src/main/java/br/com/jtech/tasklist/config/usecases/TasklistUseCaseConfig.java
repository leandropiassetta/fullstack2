package br.com.jtech.tasklist.config.usecases;

import br.com.jtech.tasklist.application.core.usecases.TasklistUseCase;
import br.com.jtech.tasklist.application.ports.output.TasklistOutputGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TasklistUseCaseConfig {

    @Bean
    public TasklistUseCase tasklistUseCase(TasklistOutputGateway tasklistOutputGateway) {
        return new TasklistUseCase(tasklistOutputGateway);
    }

}
