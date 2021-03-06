package io.github.jangalinski.camunda.camel

import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.camunda.bpm.camel.component.CamundaBpmComponent
import org.camunda.bpm.camel.component.CamundaBpmConstants
import org.camunda.bpm.camel.spring.CamelServiceImpl
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.event.EventListener

@SpringBootApplication
@EnableProcessApplication
class CamundaCamelApplication {

  companion object {
    val log: Logger = LoggerFactory.getLogger(CamundaCamelApplication::class.java)
  }

  @Autowired
  lateinit var camelContext: CamelContext

  @Bean
  fun camel(processEngine: ProcessEngine, camelContext: CamelContext) =
    CamelServiceImpl().apply {
      setCamelContext(camelContext)
      setProcessEngine(processEngine)

      camelContext.addComponent(CamundaBpmConstants.CAMUNDA_BPM_CAMEL_URI_SCHEME, CamundaBpmComponent(processEngine))
    }

  @EventListener
  fun init(evt: PostDeployEvent) {

    // TODO Kotlin DSL
    evt.processEngine.repositoryService.createDeployment()
      .addModelInstance("dummy.bpmn",
        Bpmn.createExecutableProcess("process_dummy")
          .startEvent()
          .serviceTask()
          .camundaExpression("\${camel.sendTo('direct:fromServiceTask')}")
          .subProcess()
          .camundaAsyncAfter()
          .embeddedSubProcess()
          .startEvent()
          .userTask()
          .endEvent()
          .subProcessDone()
          .endEvent()
          .done())
      .deploy()

    log.info("Successfully started Camel with components: " + camelContext.componentNames);
    log.info("=======================");
  }

  @Bean
  fun processStarter() = object : RouteBuilder() {
    override fun configure() {
      from("timer:hello?period=4000&delay=3000")
        .description("starts an instance of process_dummy every second")
        .routeId("dummy")
        .routeGroup("processes")
        .process { e -> e.getIn().body = "Hello Camel!" }
        .to("log:io.github.jangalinski?level=INFO")
        .to("camunda-bpm://start?processDefinitionKey=process_dummy&copyBodyAsVariable=helloVar")

      from("direct:fromServiceTask").to("log:io.github.jangalinski.camunda.camel?showAll=true&multiline=true");
    }
  }
}

fun main(args: Array<String>) {
  runApplication<CamundaCamelApplication>(*args)
}

