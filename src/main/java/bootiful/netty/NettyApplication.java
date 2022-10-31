package bootiful.netty;

import org.reflections.Reflections;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
public class NettyApplication {

    public static void main(String[] args) {
        SpringApplication.run(NettyApplication.class, args);
    }
}


@Configuration
class GatewayConfiguration {

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(s -> s
                        .path("/proxy")
                        .filters(fs -> fs
                                .setPath("/sample-json.json")
                                .addResponseHeader("X-Josh-Loves-Gateway", "true")
                                .addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        )
                        .uri("https://tools.learningcontainer.com/"))
                .build();
    }


    @Bean
    static BeanFactoryInitializationAotProcessor gatewayBeanFactoryInitializationAotProcessor() {
        return beanFactory -> (gc, beanFactoryInitializationCode) -> {
            var hints = gc.getRuntimeHints().reflection();
            var all = getConfigurableTypes(beanFactory);
            var mcs = MemberCategory.values();
            for (var c : all) {
                hints.registerType(c, mcs);
            }
        };
    }

    private static Set<Class<?>> getConfigurableTypes(BeanFactory beanFactory) {
        var classesToAdd = new HashSet<Class<? extends Configurable>>();
        var genericsToAdd = new HashSet<Class<?>>();

        for (var pkg : getAllPackages(beanFactory)) {
            var reflections = new Reflections(pkg);
            var subs = reflections.getSubTypesOf(Configurable.class);
            classesToAdd.addAll(subs);
        }

        for (var c : classesToAdd) {
            var rt = ResolvableType.forClass(c);
            if (rt.getSuperType().hasGenerics()) {
                var gens = rt.getSuperType().getGenerics();
                for (var g : gens) {
                    genericsToAdd.add(g.toClass());
                }
            }
        }
        var all = new HashSet<Class<?>>();
        all.addAll(classesToAdd);
        all.addAll(genericsToAdd);
        return all.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static Set<String> getAllPackages(BeanFactory factory) {
        var packages = new HashSet<String>();
        packages.add("org.springframework.cloud.gateway");
        packages.addAll(AutoConfigurationPackages.get(factory));
        return packages;
    }
}

@Controller
@ResponseBody
class GreetingsController {

    @GetMapping("/hello")
    Map<String, String> hello() {
        return Map.of("message", "Hello, world!");
    }
}
