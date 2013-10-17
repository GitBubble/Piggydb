package marubinotto.piggydb.extension;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletContext;

import marubinotto.util.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.springframework.context.ApplicationContext;

public class ExtensionDeployer {
	
	private static Log logger = LogFactory.getLog(ExtensionDeployer.class);
	
	public static final String DEF_FILE_NAME = "META-INF/piggydb-extension-class";
	public static final String WEBAPP_DIR = "META-INF/webapp";

	public static Enumeration<URL> allDefFiles() throws IOException {
		return getResources(DEF_FILE_NAME);
	}
	
	private static Enumeration<URL> getResources(String path) throws IOException {
		return ExtensionDeployer.class.getClassLoader().getResources(path);
	}
	
	public static void testClassLoaderResources() throws IOException {
		logger.info("Testing the classloader: " + ExtensionDeployer.class.getClassLoader().getClass().getName());
		checkResources("org/apache/commons");
		checkResources("META-INF");
		checkResources(WEBAPP_DIR);
		checkResources(DEF_FILE_NAME);
	}
	
	private static void checkResources(String path) throws IOException {
		logger.info("Checking the path: " + path);
		Enumeration<URL> resources = getResources(path);
		if (!resources.hasMoreElements()) {
			logger.warn(" Couldn't get resources");
		}
		while (resources.hasMoreElements()) {
			logger.info(" - " + resources.nextElement().toExternalForm());
		}
	}
	
	public static void deployWebappFiles(ServletContext servletContext) 
	throws IOException {
		Assert.Arg.notNull(servletContext, "servletContext");
		
		FileSystemManager fsManager = VFS.getManager();
		FileObject webappDir = fsManager.resolveFile(servletContext.getRealPath("/"));
		logger.info("Webapp dir: " + webappDir.getName());
		
		for (Enumeration<URL> dirUrls = getResources(WEBAPP_DIR); dirUrls.hasMoreElements();) {
			FileObject extWebappDir = fsManager.resolveFile(dirUrls.nextElement().toExternalForm());
			logger.info("Extension webapp dir: " + extWebappDir.getName());
			if (extWebappDir.getType().hasChildren()) {
				logger.info("  Deploying webapp files ... ");
				webappDir.copyFrom(extWebappDir, new AllFileSelector());
			}
		}
	}
	
	public static void initAll(
		ServletContext servletContext,
		ApplicationContext appContext) 
	throws IOException {
		Assert.Arg.notNull(servletContext, "servletContext");
		Assert.Arg.notNull(appContext, "appContext");

		// initialize extensions
		for (Enumeration<URL> files = allDefFiles(); files.hasMoreElements();) {
			URL defFile = files.nextElement();
			try {
				initExtension(defFile, servletContext, appContext);
			}
			catch (Exception e) {
				logger.error("Extension initialization error: " + defFile.toExternalForm(), e);
			}
		}
	}
	
	private static void initExtension(
		URL defFile, 
		ServletContext servletContext,
		ApplicationContext appContext) 
	throws Exception {
		String className = IOUtils.toString(defFile).trim();
		logger.info("Initializing extension: " + className);
		Extension extension = (Extension)Class.forName(className).newInstance();
		extension.init(servletContext, appContext);
	}
}
