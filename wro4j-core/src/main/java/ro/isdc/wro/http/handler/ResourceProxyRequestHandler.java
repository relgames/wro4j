package ro.isdc.wro.http.handler;

import com.sun.org.apache.bcel.internal.generic.RETURN;
import com.sun.xml.internal.ws.encoding.ContentType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.config.Context;
import ro.isdc.wro.http.support.UnauthorizedRequestException;
import ro.isdc.wro.model.group.Inject;
import ro.isdc.wro.model.resource.locator.factory.UriLocatorFactory;

import javax.activation.FileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides access to wro resources via a resource proxy.
 *
 * @author Ivar Conradi Østhus
 * @created 19 May 2012
 * @since 1.4.7
 */
public class ResourceProxyRequestHandler implements RequestHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ResourceProxyRequestHandler.class);

  public static final String PARAM_RESOURCE_ID = "id";
  public static final String PATH_RESOURCES = "wroResources";
  
  @Inject
  private UriLocatorFactory uriLocatorFactory;

  private FileTypeMap fileTypeMap = FileTypeMap.getDefaultFileTypeMap();

  public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final String resourceUri = request.getParameter(PARAM_RESOURCE_ID);
    if(!hasAccessToResource(resourceUri)) {
      accessDeniedResponse(resourceUri, response);
    }

    OutputStream outputStream = response.getOutputStream();
    LOG.debug("locating stream for resourceId: {}", resourceUri);
    final InputStream is = uriLocatorFactory.locate(resourceUri);
    if (is == null) {
      throw new WroRuntimeException("Cannot process request with uri: " + request.getRequestURI());
    }

    int length = IOUtils.copy(is, outputStream);
    IOUtils.closeQuietly(is);
    IOUtils.closeQuietly(outputStream);

    response.setContentLength(length);
    response.setContentType(getContentType(resourceUri));
  }

  /**
   * {@inheritDoc}
   */
  public boolean accept(HttpServletRequest request) {
    return StringUtils.contains(request.getRequestURI(), PATH_RESOURCES);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isEnabled() {
    return true;
  }

  private void accessDeniedResponse(String resourceUri, HttpServletResponse response) {
    throw new UnauthorizedRequestException("Unauthorized resource request detected! " + resourceUri);
  }

  /**
   * TODO: use new AuthorizedResourcesHolder to check acccess to resourceUri
   * Verifies that the user has access or not to the requested resource
   *
   * @param resourceUri of the resource to be proxied.
   * @return
   */
  private boolean hasAccessToResource(String resourceUri) {
    return true;
  }

  private String getContentType(String resourceUri) {
    if(resourceUri.toLowerCase().endsWith(".css")) {
      return "text/css";
    } else if(resourceUri.toLowerCase().endsWith(".js")) {
      return "application/javascript";
    } else if(resourceUri.toLowerCase().endsWith(".png")) {
      return "image/png";
    } else {
      return fileTypeMap.getContentType(resourceUri);
    }
  }

}