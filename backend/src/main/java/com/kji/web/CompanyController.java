package com.kji.web;

import com.kji.company.Company;
import com.kji.company.CompanyAliasRepository;
import com.kji.company.CompanyIdentifierRepository;
import com.kji.company.CompanyMetricRepository;
import com.kji.company.CompanyRepository;
import com.kji.company.CompanyRiskReasonRepository;
import com.kji.job.JobRepository;
import com.kji.job.LifecycleState;
import com.kji.web.dto.CompanyResponse;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final CompanyAliasRepository aliasRepository;
    private final CompanyIdentifierRepository identifierRepository;
    private final CompanyMetricRepository metricRepository;
    private final CompanyRiskReasonRepository riskReasonRepository;
    private final JobRepository jobRepository;

    public CompanyController(CompanyRepository companyRepository,
                             CompanyAliasRepository aliasRepository,
                             CompanyIdentifierRepository identifierRepository,
                             CompanyMetricRepository metricRepository,
                             CompanyRiskReasonRepository riskReasonRepository,
                             JobRepository jobRepository) {
        this.companyRepository = companyRepository;
        this.aliasRepository = aliasRepository;
        this.identifierRepository = identifierRepository;
        this.metricRepository = metricRepository;
        this.riskReasonRepository = riskReasonRepository;
        this.jobRepository = jobRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CompanyResponse> list(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size) {
        return companyRepository
                .findAllByOrderByCanonicalNameAsc(
                        PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size))))
                .stream()
                .map(company -> CompanyResponse.summary(company, openJobCount(company.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CompanyResponse detail(@PathVariable Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No company with id " + id));
        return CompanyResponse.detail(company, openJobCount(id),
                aliasRepository.findByCompanyId(id),
                identifierRepository.findByCompanyId(id),
                metricRepository.findByCompanyIdOrderByObservedAtDesc(id),
                riskReasonRepository.findByCompanyIdOrderByAssessedAtDesc(id));
    }

    private long openJobCount(Long companyId) {
        return jobRepository.findByCompanyId(companyId).stream()
                .filter(job -> job.getLifecycleState() != LifecycleState.CLOSED)
                .count();
    }
}
