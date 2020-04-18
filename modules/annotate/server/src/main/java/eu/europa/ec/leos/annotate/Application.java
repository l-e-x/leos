/*
 * Copyright 2019 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.annotate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
import eu.europa.ec.leos.annotate.services.impl.AuthenticatedUserStore;
import eu.europa.ec.leos.annotate.websockets.AnnotateWebSocketHandler;
import eu.europa.ec.leos.annotate.websockets.MessageBroker;
import eu.europa.ec.leos.annotate.websockets.WebSessionRegistry;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.ThreadLocalTargetSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
@Configuration
@EnableSpringDataWebSupport
@EnableAspectJAutoProxy
@EnableWebSocket
@EnableScheduling
@EnableJpaRepositories(basePackages = "eu.europa.ec.leos.annotate")
@SuppressFBWarnings(value = "RI_REDUNDANT_INTERFACES")
public class Application extends SpringBootServletInitializer implements WebApplicationInitializer {

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder application) {
        return application
                .properties("spring.config.name:anot")
                .sources(Application.class);
    }

    public static void main(final String[] args) {
        new SpringApplicationBuilder()
                .properties("spring.config.name:anot")
                .sources(Application.class)
                .run(args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new StaticWebMvcConfigurerAdapter();
    }

    // placed into a static class to follow SpotBugs rules
    private static class StaticWebMvcConfigurerAdapter extends WebMvcConfigurerAdapter {
        @Override
        public void addCorsMappings(final CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins("*") // origin could be fine-tuned here in the future once the environments are stable
                    .allowedMethods("*");
        }

        @Override
        public void configurePathMatch(final PathMatchConfigurer configurer) {
            super.configurePathMatch(configurer);
            // turn off all suffix pattern matching
            configurer.setUseSuffixPatternMatch(false);
        }

        @Override
        public void addResourceHandlers (final ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/client/**").addResourceLocations("/client/")
                    .setCachePeriod(0);
        }
    }

    /**
     * bean for launching REST calls (used for accessing UD repo)
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * beans for storing the currently authenticated user thread-safe
     */
    @Bean(destroyMethod = "destroy")
    public ThreadLocalTargetSource threadLocalAuthenticatedUserStore() {
        final ThreadLocalTargetSource result = new ThreadLocalTargetSource();
        result.setTargetBeanName("authenticatedUserStore");
        return result;
    }

    @Primary
    @Bean(name = "proxiedThreadLocalTargetSource")
    public ProxyFactoryBean proxiedThreadLocalTargetSource(final ThreadLocalTargetSource threadLocTS) {
        final ProxyFactoryBean result = new ProxyFactoryBean();
        result.setTargetSource(threadLocTS);
        return result;
    }

    @Bean(name = "authenticatedUserStore")
    @Scope(scopeName = "prototype")
    public AuthenticatedUserStore authenticatedUserStore() {
        return new AuthenticatedUserStore();
    }

    @Configuration
    private static class WebSocketConfig implements WebSocketConfigurer {

        final private AuthenticationService authService;
        final private WebSessionRegistry webSessionRegistry;
        final private MessageBroker messageBroker;

        @Autowired
        WebSocketConfig(final AuthenticationService authService, final WebSessionRegistry webSessionRegistry, 
                final MessageBroker messageBroker) {
            this.authService = authService;
            this.webSessionRegistry = webSessionRegistry;
            this.messageBroker = messageBroker;
        }

        @Override
        public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
            registry.addHandler(webSocketHandler(), "/ws").setAllowedOrigins("*");
        }

        @Bean
        public AnnotateWebSocketHandler webSocketHandler() {
            return new AnnotateWebSocketHandler(authService, webSessionRegistry, messageBroker);
        }

    }
}