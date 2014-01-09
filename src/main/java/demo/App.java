package demo;

import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class App {
	
	@Path("/hello")
	public static class Resource {
		
		@Inject Counter counter;
			
		@GET
		public String get() {
			return "Hello, User number " + counter.getNext();
		}
	}
	
	@Singleton
	public static class Counter {
		private final AtomicInteger counter = new AtomicInteger(0);
		public int getNext() {
			return counter.incrementAndGet();
		}
	}

	public static class Config extends GuiceServletContextListener {
		@Override
		protected Injector getInjector() {
			return Guice.createInjector(new ServletModule(){
				@Override
				protected void configureServlets() {
                    // excplictly bind GuiceContainer before binding Jersey resources
                    // otherwise resource won't be available for GuiceContainer
                    // when using two-phased injection
                    bind(GuiceContainer.class);

                    // bind Jersey resources
                    PackagesResourceConfig resourceConfig = new PackagesResourceConfig("demo");
                    for (Class<?> resource : resourceConfig.getClasses()) {
                        bind(resource);
                    }

                    // Serve resources with Jerseys GuiceContainer
                    serve("/*").with(GuiceContainer.class);
				}		
			});
		}		
	}
	
	@SuppressWarnings("serial")
	public static class DummySevlet extends HttpServlet { }
	
	public static void main(String[] args) throws Exception {
		int port = Integer.valueOf(System.getProperty("port"));
		GrizzlyWebServer server = new GrizzlyWebServer(port);
		ServletAdapter adapter = new ServletAdapter(new DummySevlet());
		adapter.addServletListener(Config.class.getName());
		adapter.addFilter(new GuiceFilter(), "GuiceFilter", null);
		server.addGrizzlyAdapter(adapter, new String[]{ "/" });
		server.start();
	}
}
