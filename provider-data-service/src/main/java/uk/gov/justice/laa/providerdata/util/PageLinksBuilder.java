package uk.gov.justice.laa.providerdata.util;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.justice.laa.providerdata.model.LinksV2;

/**
 * Builds a {@link LinksV2} pagination envelope for a paginated list response.
 *
 * <p>Link URIs are derived from the current HTTP request URI using {@link
 * UriComponentsBuilder#fromCurrentRequest()}, so this must be called within a request-handling
 * thread. {@code page} and {@code pageSize} query parameters in the original request are replaced
 * with the computed values for each link.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * LinksV2 links = PageLinksBuilder.build(pageIndex, pageSize, totalPages);
 * }</pre>
 */
public final class PageLinksBuilder {

  private PageLinksBuilder() {}

  /**
   * Builds pagination links for the given page position.
   *
   * @param page zero-based current page index
   * @param pageSize number of items per page
   * @param totalPages total number of pages returned by the query
   * @return a {@link LinksV2} with {@code self}, {@code first}, {@code last}, and (where
   *     applicable) {@code prev} and {@code next} links
   */
  public static LinksV2 build(int page, int pageSize, int totalPages) {
    UriComponentsBuilder base =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .replaceQueryParam("page")
            .replaceQueryParam("pageSize");

    LinksV2 links =
        new LinksV2().self(pageUri(base, page, pageSize)).first(pageUri(base, 0, pageSize));

    if (totalPages > 0) {
      links.last(pageUri(base, totalPages - 1, pageSize));
    }
    if (page > 0) {
      links.prev(pageUri(base, page - 1, pageSize));
    }
    if (page < totalPages - 1) {
      links.next(pageUri(base, page + 1, pageSize));
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
