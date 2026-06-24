package com.smartcampus.library;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@WebService(serviceName = "LibraryService", targetNamespace = "http://library.smartcampus.com/")
@Service
public class LibrarySoapService {

    @Autowired
    private BookRepository bookRepository;

    @WebMethod(operationName = "checkBookAvailability")
    public Book checkBookAvailability(@WebParam(name = "bookId") String bookId) throws Exception {
        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (bookOpt.isEmpty()) {
            throw new Exception("SOAP FAULT: Book with ID " + bookId + " does not exist in the legacy system.");
        }

        return bookOpt.get();
    }

    @WebMethod(operationName = "addLegacyBook")
    public String addLegacyBook(@WebParam(name = "bookId") String bookId,
            @WebParam(name = "title") String title,
            @WebParam(name = "author") String author) {
        Book book = new Book();
        book.setBookId(bookId);
        book.setTitle(title);
        book.setAuthor(author);
        book.setStatus("AVAILABLE");

        bookRepository.save(book);
        return "Book successfully added to legacy system.";
    }
}