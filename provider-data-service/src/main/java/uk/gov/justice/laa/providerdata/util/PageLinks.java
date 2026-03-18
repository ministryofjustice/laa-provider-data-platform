package uk.gov.justice.laa.providerdata.util;

import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.justice.laa.providerdata.model.LinksV2;

/**
 * Static factory for {@link LinksV2} pagination navigation links.
 *
 * <p>Link URIs are derived from the current HTTP request URI using {@link
 * UriComponentsBuilder#fromCurrentRequest()}, so {@link #of} must be called within a
 * request-handling thread. {@code page} and {@code pageSize} query parameters in the original
 * request are replaced with the computed values for each link.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * LinksV2 links = PageLinks.of(pageParams, page.getTotalPages());
 * }</pre>
 */
public final class PageLinks {

  private PageLinks() {}

  /**
   * Creates pagination links for the given page result.
   *
   * @param page the page result from the service layer
   * @return a {@link LinksV2} with {@code self}, {@code first}, {@code last}, and (where
   *     applicable) {@code prev} and {@code next} links
   */
  public static LinksV2 of(Page<?> page) {
    int pageNumber = page.getNumber();
    int pageSize = page.getSize();
    int totalPages = page.getTotalPages();
    UriComponentsBuilder base =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .replaceQueryParam("page")
            .replaceQueryParam("pageSize");

    LinksV2 links =
        new LinksV2().self(pageUri(base, pageNumber, pageSize)).first(pageUri(base, 0, pageSize));

    if (totalPages > 0) {
      links.last(pageUri(base, totalPages - 1, pageSize));
    }
    if (pageNumber > 0) {
      links.prev(pageUri(base, pageNumber - 1, pageSize));
    }
    if (pageNumber < totalPages - 1) {
      links.next(pageUri(base, pageNumber + 1, pageSize));
    }

    return links;
  }

  private static URI pageUri(UriComponentsBuilder base, int page, int pageSize) {
    return base.cloneBuilder()
        .queryParam("page", page)
        .queryParam("pageSize", pageSize)
        .build()
        .toUri();
  }
}
