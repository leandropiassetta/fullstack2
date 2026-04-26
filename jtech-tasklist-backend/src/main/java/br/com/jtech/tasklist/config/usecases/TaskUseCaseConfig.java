package br.com.jtech.tasklist.config.usecases;

import br.com.jtech.tasklist.application.core.usecases.TaskUseCase;
import br.com.jtech.tasklist.application.ports.output.TaskOutputGateway;
import br.com.jtech.tasklist.application.ports.output.TasklistOutputGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskUseCaseConfig {

    @Bean
    public TaskUseCase taskUseCase(TaskOutputGateway taskOutputGateway,
                                   TasklistOutputGateway tasklistOutputGateway) {
        return new TaskUseCase(taskOutputGateway, tasklistOutputGateway);
    }

}
