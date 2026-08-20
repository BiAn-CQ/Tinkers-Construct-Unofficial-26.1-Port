package slimeknights.tconstruct.client;

import net.minecraft.resources.Identifier;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.BookScreenOpener;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.repository.FileRepository;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.tconstruct.library.client.book.TinkerBook;
import slimeknights.tconstruct.shared.item.TinkerBookItem.BookType;

import static slimeknights.tconstruct.library.TinkerBookIDs.ENCYCLOPEDIA_ID;
import static slimeknights.tconstruct.library.TinkerBookIDs.FANTASTIC_FOUNDRY_ID;
import static slimeknights.tconstruct.library.TinkerBookIDs.MATERIALS_BOOK_ID;
import static slimeknights.tconstruct.library.TinkerBookIDs.MIGHTY_SMELTING_ID;
import static slimeknights.tconstruct.library.TinkerBookIDs.PUNY_SMELTING_ID;
import static slimeknights.tconstruct.library.TinkerBookIDs.TINKERS_GADGETRY_ID;

/**
 * Client bridge for books while the full legacy book content tree is still excluded from the 26.1 source set.
 *
 * <p>The shared item only exposes this through the client-only use path. Registering the repositories lazily keeps
 * the server class path free of book implementation classes while ensuring a book item never resolves to null after
 * the resource reload has completed.</p>
 */
public final class TConstructBookCompat {
  private static boolean initialized;

  private TConstructBookCompat() {}

  /** Initializes Tinkers' book page types exactly once on the client. */
  public static void initialize() {
    if (!initialized) {
      TinkerBook.initBook();
      initialized = true;
    }
  }

  /** Resolves the book represented by an item, creating the basic resource-backed entry if needed. */
  public static BookScreenOpener getBook(BookType bookType) {
    initialize();
    Identifier id = getId(bookType);
    BookData book = TinkerBook.getBook(bookType);
    // Keep a fallback for optional classpath combinations where the staged
    // book implementation is not present, but normally the full TConstruct
    // registrations above provide custom page types and transformers.
    if (book == null) {
      book = BookLoader.getBook(id);
    }
    if (book == null) {
      book = BookLoader.registerBook(id, false, false,
        new FileRepository(Identifier.fromNamespaceAndPath(id.getNamespace(), "book/" + id.getPath())));
    }
    // Load before installing the font: BookData.load() clears a cached
    // uniform font when the book appearance does not request it. PageContent
    // still needs a font for titles and wrapping even when the appearance is
    // using the normal font.
    book.load();
    // Match the original client setup's Unicode-capable book font after the
    // Mantle load has completed. Mantle now installs a normal fallback before
    // running transformers, so this assignment is safe for page rendering.
    book.fontRenderer = BookScreen.getUniformFont();
    return book;
  }

  private static Identifier getId(BookType bookType) {
    return switch (bookType) {
      case MATERIALS_AND_YOU -> MATERIALS_BOOK_ID;
      case PUNY_SMELTING -> PUNY_SMELTING_ID;
      case MIGHTY_SMELTING -> MIGHTY_SMELTING_ID;
      case TINKERS_GADGETRY -> TINKERS_GADGETRY_ID;
      case FANTASTIC_FOUNDRY -> FANTASTIC_FOUNDRY_ID;
      case ENCYCLOPEDIA -> ENCYCLOPEDIA_ID;
    };
  }
}
