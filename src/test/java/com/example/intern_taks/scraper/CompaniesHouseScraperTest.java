package com.example.intern_taks.scraper;

import com.example.intern_taks.domain.Officer;
import com.example.intern_taks.domain.PersonWithSignificantControl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompaniesHouseScraperTest {

    private final CompaniesHouseScraper scraper =
            new CompaniesHouseScraper();

    @Test
    void extractsOnlyCompanyLinksFromSearchResults() {
        Document document = Jsoup.parse("""
                <main>
                    <a href="/company/03058989">TESLA LIMITED</a>
                    <a href="/company/OC448855">TESLA ASSORTMENT LLP</a>
                    <a href="/company/03058989/officers">Not a company result</a>
                </main>
                """);

        Map<String, String> links = scraper.extractCompanyLinks(document);

        assertEquals(Map.of(
                "03058989", "TESLA LIMITED",
                "OC448855", "TESLA ASSORTMENT LLP"
        ), links);
    }

    @Test
    void extractsCleanOfficerRolesAndRemovesDuplicates() {
        Document document = Jsoup.parse("""
                <main>
                    <div class="appointment">
                        <a href="/officers/first/appointments">DOE, Jane</a>
                        <dl>
                            <dt>Role Active</dt><dd>Director</dd>
                            <dt>Appointed on</dt><dd>7 July 2020</dd>
                            <dt>Nationality</dt><dd>British</dd>
                        </dl>
                    </div>
                    <div class="appointment">
                        <a href="/officers/first/appointments">DOE, Jane</a>
                        <dl>
                            <dt>Role Active</dt><dd>Director</dd>
                            <dt>Appointed on</dt><dd>7 July 2020</dd>
                        </dl>
                    </div>
                    <div class="appointment">
                        <a href="/officers/second/appointments">SMITH, John</a>
                        <dl>
                            <dt>Role Resigned</dt><dd>Secretary</dd>
                            <dt>Appointed on</dt><dd>1 January 2019</dd>
                        </dl>
                    </div>
                </main>
                """);

        List<Officer> officers = scraper.parseOfficers(document);

        assertEquals(2, officers.size());
        assertEquals("DOE, Jane", officers.get(0).getName());
        assertEquals("Director", officers.get(0).getRole());
        assertEquals(
                LocalDate.of(2020, 7, 7),
                officers.get(0).getAppointmentDate()
        );
        assertEquals("Secretary", officers.get(1).getRole());
    }

    @Test
    void extractsOnlyPscCardsAndRemovesStatusSuffix() {
        Document document = Jsoup.parse("""
                <main>
                    <h2>Persons with significant control</h2>
                    <section class="psc-card">
                        <h2>Dr Michael Colin Begg Active</h2>
                        <dl>
                            <dt>Nature of control</dt>
                            <dd>Has significant influence or control</dd>
                        </dl>
                    </section>
                    <section class="psc-card">
                        <h2>Storrington Equityco Limited Active</h2>
                        <dl>
                            <dt>Nature of control</dt>
                            <dd>Ownership of shares - 75% or more</dd>
                        </dl>
                    </section>
                    <h2>Support links</h2>
                </main>
                <footer><h2>Cookies on Companies House services</h2></footer>
                """);

        List<PersonWithSignificantControl> pscs =
                scraper.parsePersonsWithSignificantControl(document);

        assertEquals(2, pscs.size());
        assertEquals("Dr Michael Colin Begg", pscs.get(0).getName());
        assertEquals(
                "Has significant influence or control",
                pscs.get(0).getNatureOfControl()
        );
        assertEquals("Storrington Equityco Limited", pscs.get(1).getName());
    }
}
