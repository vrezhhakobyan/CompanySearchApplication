package com.example.intern_taks.scraper;

import com.example.intern_taks.domain.Company;
import com.example.intern_taks.domain.Officer;
import com.example.intern_taks.domain.PersonWithSignificantControl;
import org.jsoup.Jsoup;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CompaniesHouseScraper {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CompaniesHouseScraper.class);

    private static final String BASE_URL =
            "https://find-and-update.company-information.service.gov.uk";

    private static final String USER_AGENT =
            "InternTaksCompanySearch/1.0";

    private static final int REQUEST_DELAY_MS = 500;

    private static final int INITIAL_RETRY_DELAY_MS = 1_000;

    private static final int MAX_FETCH_ATTEMPTS = 3;

    private static final int TIMEOUT_MS = 15_000;

    private static final int MAX_COMPANIES = 100;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy");

    public List<Company> searchCompanies(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String url = BASE_URL + "/search/companies?q=" +
                encodeQuery(query);

        Document searchDocument = fetch(url);

        Map<String, String> companyLinks = extractCompanyLinks(searchDocument);

        List<Company> companies = new ArrayList<>();

        int count = 0;

        for (Map.Entry<String, String> entry : companyLinks.entrySet()) {

            if (count >= MAX_COMPANIES) {
                break;
            }

            String companyNumber = entry.getKey();

            try {
                Company company = scrapeCompany(companyNumber);

                if (company != null) {
                    companies.add(company);
                    count++;
                }
            } catch (Exception exception) {
                LOGGER.warn(
                        "Skipping company {} because it could not be scraped: {}",
                        companyNumber,
                        exception.getMessage()
                );
            }
        }

        return companies;
    }

    private Company scrapeCompany(String companyNumber) {

        Document overviewDocument = fetch(
                BASE_URL + "/company/" + companyNumber
        );

        String name = extractCompanyName(overviewDocument);

        String status = extractLabeledValue(
                overviewDocument,
                "Company status"
        );

        String companyType = extractLabeledValue(
                overviewDocument,
                "Company type"
        );

        String incorporationDateText = extractLabeledValue(
                overviewDocument,
                "Incorporated on"
        );

        String dissolutionDateText = extractLabeledValue(
                overviewDocument,
                "Dissolved on"
        );

        String registeredOfficeAddress = extractLabeledValue(
                overviewDocument,
                "Registered office address"
        );

        Company company = new Company();

        company.setCompanyNumber(companyNumber);
        company.setName(name);
        company.setStatus(status);
        company.setCompanyType(companyType);
        company.setIncorporationDate(parseDate(incorporationDateText));
        company.setDissolutionDate(parseDate(dissolutionDateText));
        company.setRegisteredOfficeAddress(registeredOfficeAddress);

        company.setOfficers(
                scrapeOfficers(companyNumber)
        );

        company.setPersonsWithSignificantControl(
                scrapePersonsWithSignificantControl(companyNumber)
        );

        return company;
    }

    private List<Officer> scrapeOfficers(String companyNumber) {

        Document document = fetch(
                BASE_URL + "/company/" + companyNumber + "/officers"
        );

        return parseOfficers(document);
    }

    List<Officer> parseOfficers(Document document) {

        List<Officer> officers = new ArrayList<>();

        Elements officerLinks = document.select(
                "a[href^=/officers/][href*=/appointments]"
        );

        for (Element officerLink : officerLinks) {

            String name = cleanText(officerLink.text());

            if (name.isBlank()) {
                continue;
            }

            Element container =
                    findOfficerContainer(officerLink);

            String role = extractOfficerRole(container);

            String appointmentDateText = extractLabeledValue(
                    container,
                    "Appointed on"
            );

            LocalDate appointmentDate =
                    parseDate(appointmentDateText);

            Officer officer = new Officer(
                    name,
                    role,
                    appointmentDate
            );

            if (!containsOfficer(officers, officer)) {
                officers.add(officer);
            }
        }

        return officers;
    }

    private List<PersonWithSignificantControl>
    scrapePersonsWithSignificantControl(String companyNumber) {

        Document document;

        try {
            document = fetch(
                    BASE_URL +
                            "/company/" +
                            companyNumber +
                            "/persons-with-significant-control"
            );
        } catch (Exception exception) {
            LOGGER.warn(
                    "Could not fetch PSC data for {}: {}",
                    companyNumber,
                    exception.getMessage()
            );

            return List.of();
        }

        return parsePersonsWithSignificantControl(document);
    }

    List<PersonWithSignificantControl>
    parsePersonsWithSignificantControl(Document document) {

        List<PersonWithSignificantControl> result =
                new ArrayList<>();

        Elements headings = document.select(
                "main h2, main h3, main h4"
        );

        for (Element heading : headings) {

            String headingText = cleanText(heading.text());

            if (!hasPscStatus(headingText)) {
                continue;
            }

            headingText = removePscStatus(headingText);

            if (headingText.equalsIgnoreCase(
                    "Persons with significant control"
            )) {
                continue;
            }

            Element container =
                    findPscContainer(heading);

            if (container == null) {
                continue;
            }

            String natureOfControl =
                    extractLabeledValue(
                            container,
                            "Nature of control"
                    );

            if (natureOfControl.isBlank()) {
                continue;
            }

            result.add(
                    new PersonWithSignificantControl(
                            headingText,
                            natureOfControl
                    )
            );
        }

        return deduplicatePsc(result);
    }

    Map<String, String> extractCompanyLinks(
            Document document
    ) {

        Map<String, String> result =
                new LinkedHashMap<>();

        Elements links = document.select(
                "a[href^=/company/]"
        );

        Pattern pattern = Pattern.compile(
                "/company/([A-Z0-9]{5,8})/?$"
        );

        for (Element link : links) {

            String href = link.attr("href");

            Matcher matcher = pattern.matcher(href);

            if (!matcher.find()) {
                continue;
            }

            String companyNumber = matcher.group(1);

            String companyName =
                    cleanText(link.text());

            result.putIfAbsent(
                    companyNumber,
                    companyName
            );

            if (result.size() >= MAX_COMPANIES) {
                break;
            }
        }

        return result;
    }

    private String extractCompanyName(
            Document document
    ) {

        Element heading = document.selectFirst(
                "main h1"
        );

        if (heading != null &&
                !heading.text().isBlank()) {

            return cleanText(heading.text());
        }

        heading = document.selectFirst("h1");

        if (heading != null) {
            return cleanText(heading.text());
        }

        return "";
    }

    private String extractLabeledValue(
            Document document,
            String label
    ) {

        if (document == null) {
            return "";
        }

        return extractLabeledValue(
                (Element) document,
                label
        );
    }

    private String extractLabeledValue(
            Element element,
            String label
    ) {

        Elements keys = element.select(
                "dt, .govuk-summary-list__key"
        );

        for (Element key : keys) {

            if (!matchesLabel(cleanText(key.text()), label)) {
                continue;
            }

            Element value = key.nextElementSibling();

            if (value != null) {
                return cleanText(value.text());
            }

            Element parent = key.parent();

            if (parent != null) {

                Element sibling =
                        parent.selectFirst(
                                "dd, " +
                                        ".govuk-summary-list__value"
                        );

                if (sibling != null) {
                    return cleanText(sibling.text());
                }
            }
        }

        return extractLabelFromText(
                element.text(),
                label
        );
    }

    private String extractOfficerRole(Element element) {

        if (element == null) {
            return "";
        }

        Elements keys = element.select(
                "dt, .govuk-summary-list__key"
        );

        for (Element key : keys) {
            if (!matchesLabel(cleanText(key.text()), "Role")) {
                continue;
            }

            Element value = key.nextElementSibling();

            if (value != null) {
                return cleanText(value.text());
            }
        }

        Pattern pattern = Pattern.compile(
                "\\bRole\\s+(?:Active|Resigned)?\\s*(.+?)" +
                        "(?=\\s+(?:Date of birth|Appointed on|" +
                        "Appointed before|Resigned on|Nationality|" +
                        "Country of residence|Identity verification)|$)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(cleanText(element.text()));

        if (matcher.find()) {
            return cleanText(matcher.group(1));
        }

        return "";
    }

    private boolean matchesLabel(String actual, String label) {
        return actual.equalsIgnoreCase(label) ||
                actual.regionMatches(
                        true,
                        0,
                        label + " ",
                        0,
                        label.length() + 1
                );
    }

    private String extractLabelFromText(
            String text,
            String label
    ) {

        String normalized = text
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ");

        Pattern pattern = Pattern.compile(
                Pattern.quote(label) +
                        "\\s*:?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(normalized);

        if (matcher.find()) {
            return cleanText(matcher.group(1));
        }

        return "";
    }

    private Element findOfficerContainer(
            Element officerLink
    ) {

        Element current = officerLink;

        for (int i = 0; i < 7 && current != null; i++) {

            String text = current.text();

            if (text.contains("Role") ||
                    text.contains("Appointed on") ||
                    text.contains("Appointment")) {

                return current;
            }

            current = current.parent();
        }

        return officerLink.parent();
    }

    private Element findPscContainer(
            Element heading
    ) {

        Element current = heading;

        for (int i = 0; i < 7 && current != null; i++) {

            if (!isPageLevelElement(current) &&
                    hasLabeledValue(
                            current,
                            "Nature of control"
                    )) {
                return current;
            }

            current = current.parent();
        }

        return null;
    }

    private boolean hasLabeledValue(
            Element element,
            String label
    ) {

        for (Element key : element.select(
                "dt, .govuk-summary-list__key"
        )) {
            if (matchesLabel(cleanText(key.text()), label)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPageLevelElement(Element element) {
        String tagName = element.tagName();

        return tagName.equals("main") ||
                tagName.equals("body") ||
                tagName.equals("html");
    }

    private String removePscStatus(String value) {
        return value.replaceFirst(
                "(?i)\\s+(active|ceased)$",
                ""
        ).trim();
    }

    private boolean hasPscStatus(String value) {
        return value.matches("(?i).*\\s+(active|ceased)$");
    }

    private List<PersonWithSignificantControl>
    deduplicatePsc(
            List<PersonWithSignificantControl> input
    ) {

        Map<String, PersonWithSignificantControl> unique =
                new LinkedHashMap<>();

        for (PersonWithSignificantControl psc : input) {

            String key =
                    psc.getName() +
                            "|" +
                            psc.getNatureOfControl();

            unique.putIfAbsent(key, psc);
        }

        return new ArrayList<>(unique.values());
    }

    private boolean containsOfficer(
            List<Officer> officers,
            Officer target
    ) {

        for (Officer officer : officers) {

            if (safeEquals(
                    officer.getName(),
                    target.getName()
            ) &&
                    safeEquals(
                            officer.getRole(),
                            target.getRole()
                    ) &&
                    safeEquals(
                            officer.getAppointmentDate(),
                            target.getAppointmentDate()
                    )) {

                return true;
            }
        }

        return false;
    }

    private boolean safeEquals(
            Object first,
            Object second
    ) {
        return first == null
                ? second == null
                : first.equals(second);
    }

    private LocalDate parseDate(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(
                    text,
                    DATE_FORMAT
            );
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private Document fetch(String url) {

        RuntimeException lastFailure = null;
        int attemptsMade = 0;

        for (int attempt = 1;
             attempt <= MAX_FETCH_ATTEMPTS;
             attempt++) {

            attemptsMade = attempt;

            try {
                Document document = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .get();

                sleep(REQUEST_DELAY_MS);

                return document;

            } catch (HttpStatusException exception) {
                lastFailure = new RuntimeException(
                        "HTTP " + exception.getStatusCode() +
                                " while fetching " + url,
                        exception
                );

                if (!isRetryableStatus(exception.getStatusCode()) ||
                        attempt == MAX_FETCH_ATTEMPTS) {
                    LOGGER.warn(
                            "Failed to fetch {} after {} attempt(s): HTTP {}",
                            url,
                            attempt,
                            exception.getStatusCode()
                    );
                    sleep(REQUEST_DELAY_MS);
                    break;
                }

                retryAfterFailure(
                        url,
                        attempt,
                        "HTTP " + exception.getStatusCode()
                );

            } catch (IOException exception) {
                lastFailure = new RuntimeException(
                        "I/O error while fetching " + url,
                        exception
                );

                if (attempt == MAX_FETCH_ATTEMPTS) {
                    LOGGER.warn(
                            "Failed to fetch {} after {} attempt(s): {}",
                            url,
                            attempt,
                            exception.getMessage()
                    );
                    sleep(REQUEST_DELAY_MS);
                    break;
                }

                retryAfterFailure(
                        url,
                        attempt,
                        exception.getClass().getSimpleName()
                );
            }
        }

        throw new RuntimeException(
                "Failed to fetch " + url +
                        " after " + attemptsMade + " attempt(s)",
                lastFailure
        );
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 ||
                (statusCode >= 500 && statusCode < 600);
    }

    private void retryAfterFailure(
            String url,
            int attempt,
            String failureDescription
    ) {

        int delayMs = INITIAL_RETRY_DELAY_MS *
                (1 << (attempt - 1));

        LOGGER.warn(
                "Fetch attempt {}/{} for {} failed ({}); retrying in {} ms",
                attempt,
                MAX_FETCH_ATTEMPTS,
                url,
                failureDescription,
                delayMs
        );

        sleep(delayMs);
    }

    private void sleep(int delayMs) {

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Request delay was interrupted",
                    exception
            );
        }
    }

    private String encodeQuery(String query) {

        return query
                .trim()
                .replace(" ", "+");
    }

    private String cleanText(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
