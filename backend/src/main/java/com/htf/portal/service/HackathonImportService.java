package com.htf.portal.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.htf.portal.dao.HackathonDao;
import com.htf.portal.dto.ImportResult;
import com.htf.portal.exception.BusinessRuleException;
import com.htf.portal.exception.CsvImportException;
import com.htf.portal.exception.DatabaseException;
import com.htf.portal.io.CsvReader;
import com.htf.portal.io.HackathonCsvParser;
import com.htf.portal.model.Hackathon;

/**
 * Admin's "import hackathons from CSV". Good lines are saved together in one manual transaction
 * (HackathonDao.insertAll). Bad lines are skipped and reported back, one message each.
 */
@Service
public class HackathonImportService {

    private static final Logger log = LoggerFactory.getLogger(HackathonImportService.class);

    private final HackathonDao hackathonDao;

    public HackathonImportService(HackathonDao hackathonDao) {
        this.hackathonDao = hackathonDao;
    }

    public ImportResult importCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("The file is empty");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BusinessRuleException("Please upload a .csv file");
        }

        CsvReader<Hackathon> csvReader = new CsvReader<>(new HackathonCsvParser());
        List<Hackathon> hackathons;
        // InputStreamReader turns the uploaded bytes into characters (UTF-8); BufferedReader gives us readLine().
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            hackathons = csvReader.readAll(reader);
        } catch (CsvImportException e) {
            // Only a wrong header line gets here. Bad data lines were collected inside the reader.
            throw new BusinessRuleException("Import stopped. " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded CSV file", e);
        }

        List<String> errors = csvReader.getErrors();
        if (hackathons.isEmpty() && errors.isEmpty()) {
            throw new BusinessRuleException("The file has a header but no data lines");
        }

        int saved = 0;
        if (!hackathons.isEmpty()) {
            try {
                saved = hackathonDao.insertAll(hackathons);
            } catch (DatabaseException e) {
                log.error("CSV import failed in the database, nothing was saved", e);
                throw new BusinessRuleException("The database rejected the import, so nothing was saved");
            }
        }

        log.info("CSV import: {} hackathons saved, {} bad lines skipped", saved, errors.size());
        if (!errors.isEmpty()) {
            log.warn("CSV import skipped lines: {}", errors);
        }
        return new ImportResult(saved, errors);
    }
}
