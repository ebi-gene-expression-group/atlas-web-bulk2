package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentpage.differential.download.DifferentialSecondaryDataFiles;
import uk.ac.ebi.atlas.experimentpage.link.LinkToArrayExpress;
import uk.ac.ebi.atlas.experimentpage.link.LinkToEga;
import uk.ac.ebi.atlas.experimentpage.link.LinkToEna;
import uk.ac.ebi.atlas.experimentpage.link.LinkToGeo;
import uk.ac.ebi.atlas.experimentpage.link.LinkToPride;
import uk.ac.ebi.atlas.experimentpage.qc.RnaSeqQcReport;
import uk.ac.ebi.atlas.model.download.ExternallyAvailableContent;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.resource.ContrastImageSupplier;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExpressionAtlasContentService {
    private final ExternallyAvailableContentService<BaselineExperiment>
            proteomicsBaselineExperimentExternallyAvailableContentService;
    private final ExternallyAvailableContentService<BaselineExperiment>
            rnaSeqBaselineExperimentExternallyAvailableContentService;
    private final ExternallyAvailableContentService<DifferentialExperiment>
            bulkDifferentialExperimentExternallyAvailableContentService;
    private final ExternallyAvailableContentService<MicroarrayExperiment>
            microarrayExperimentExternallyAvailableContentService;

    private final LinkToEna linkToEna;
    private final LinkToEga linkToEga;
    private final LinkToGeo linkToGeo;
    private final LinkToPride linkToPride;
    private final LinkToArrayExpress linkToArrayExpress;

    private final ExperimentTrader experimentTrader;

    @Autowired
    public ExpressionAtlasContentService(
            ExperimentDownloadSupplier.Proteomics proteomicsExperimentDownloadSupplier,
            ExperimentDownloadSupplier.RnaSeqBaseline rnaSeqBaselineExperimentDownloadSupplier,
            ExperimentDownloadSupplier.BulkDifferential bulkDifferential,
            ExperimentDownloadSupplier.Microarray microarrayExperimentDownloadSupplier,
            ContrastImageSupplier.RnaSeq rnaSeqDifferentialContrastImageSupplier,
            ContrastImageSupplier.Microarray microarrayContrastImageSupplier,
            StaticFilesDownload.Baseline baselineStaticFilesDownload,
            StaticFilesDownload.RnaSeq rnaSeqDifferentialStaticFilesDownload,
            StaticFilesDownload.Microarray microarrayStaticFilesDownload,
            DifferentialSecondaryDataFiles.RnaSeq rnaSeqDifferentialSecondaryDataFiles,
            DifferentialSecondaryDataFiles.Microarray microarraySecondaryDataFiles,
            ExperimentDesignFile.Baseline baselineExperimentDesignFile,
            ExperimentDesignFile.RnaSeq rnaSeqDifferentialExperimentDesignFile,
            ExperimentDesignFile.Microarray microarrayExperimentDesignFile,
            RnaSeqQcReport rnaSeqQCReport,
            LinkToArrayExpress linkToArrayExpress,
            LinkToPride linkToPride,
            LinkToEna linkToEna,
            LinkToEga linkToEga,
            LinkToGeo linkToGeo,
            ExperimentTrader experimentTrader) {
        this.experimentTrader = experimentTrader;
        this.linkToEna = linkToEna;
        this.linkToEga = linkToEga;
        this.linkToGeo = linkToGeo;
        this.linkToPride = linkToPride;
        this.linkToArrayExpress = linkToArrayExpress;
        this.proteomicsBaselineExperimentExternallyAvailableContentService =
                new ExternallyAvailableContentService<>(
                        ImmutableList.of(
                                proteomicsExperimentDownloadSupplier,
                                baselineStaticFilesDownload,
                                baselineExperimentDesignFile));

        this.rnaSeqBaselineExperimentExternallyAvailableContentService =
                new ExternallyAvailableContentService<>(
                        ImmutableList.of(
                                rnaSeqBaselineExperimentDownloadSupplier,
                                baselineStaticFilesDownload,
                                baselineExperimentDesignFile));

        this.bulkDifferentialExperimentExternallyAvailableContentService =
                new ExternallyAvailableContentService<>(
                        ImmutableList.of(
                                bulkDifferential,
                                rnaSeqDifferentialSecondaryDataFiles,
                                rnaSeqDifferentialStaticFilesDownload,
                                rnaSeqDifferentialExperimentDesignFile,
                                rnaSeqQCReport,
                                rnaSeqDifferentialContrastImageSupplier));

        this.microarrayExperimentExternallyAvailableContentService =
                new ExternallyAvailableContentService<>(
                        ImmutableList.of(
                                microarrayExperimentDownloadSupplier,
                                microarraySecondaryDataFiles,
                                microarrayStaticFilesDownload,
                                microarrayExperimentDesignFile,
                                microarrayContrastImageSupplier));
    }

    public Function<HttpServletResponse, Void> stream(String experimentAccession, String accessKey, final URI uri) {
        Experiment<?> experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        if (experiment.getType().isProteomicsBaseline()) {
            return proteomicsBaselineExperimentExternallyAvailableContentService.stream(
                    (BaselineExperiment) experiment, uri);
        } else if (experiment.getType().isRnaSeqBaseline()) {
            return rnaSeqBaselineExperimentExternallyAvailableContentService.stream(
                    (BaselineExperiment) experiment, uri);
        } else if (experiment.getType().isBulkDifferential()) {
            return bulkDifferentialExperimentExternallyAvailableContentService.stream(
                    (DifferentialExperiment) experiment, uri);
        } else {
            return microarrayExperimentExternallyAvailableContentService.stream(
                    (MicroarrayExperiment) experiment, uri);
        }
    }

    public ImmutableList<ExternallyAvailableContent> list(String experimentAccession,
                                      String accessKey,
                                      ExternallyAvailableContent.ContentType contentType) {
        Experiment<?> experiment = experimentTrader.getExperiment(experimentAccession, accessKey);
        ImmutableList.Builder<ExternallyAvailableContent> externalResourcesLinks = ImmutableList.builder();
        ImmutableList.Builder<ExternallyAvailableContent> otherExternalResourceLinks;

        switch (experiment.getType()) {
            case PROTEOMICS_BASELINE:
            case PROTEOMICS_BASELINE_DIA:
                externalResourcesLinks.addAll(proteomicsBaselineExperimentExternallyAvailableContentService.list((BaselineExperiment) experiment, contentType));
                otherExternalResourceLinks = externalResourceLinks(experiment);
                externalResourcesLinks.addAll(otherExternalResourceLinks.build());
                break;
            case RNASEQ_MRNA_BASELINE:
                externalResourcesLinks.addAll(rnaSeqBaselineExperimentExternallyAvailableContentService.list(
                        (BaselineExperiment) experiment, contentType));
                otherExternalResourceLinks = externalResourceLinks(experiment);
                externalResourcesLinks.addAll(otherExternalResourceLinks.build());
                break;
            case RNASEQ_MRNA_DIFFERENTIAL:
            case PROTEOMICS_DIFFERENTIAL:
                externalResourcesLinks.addAll(bulkDifferentialExperimentExternallyAvailableContentService.list(
                        (DifferentialExperiment) experiment, contentType));
                otherExternalResourceLinks = externalResourceLinks(experiment);
                externalResourcesLinks.addAll(otherExternalResourceLinks.build());
                break;
            case MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL:
            case MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL:
            case MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL:
                externalResourcesLinks.addAll(microarrayExperimentExternallyAvailableContentService.list((MicroarrayExperiment) experiment, contentType));
                otherExternalResourceLinks = externalResourceLinks(experiment);
                externalResourcesLinks.addAll(otherExternalResourceLinks.build());
                break;
            default:
                throw new IllegalArgumentException(experiment.getType() + ": experiment type not supported.");
        }

        if (experimentAccession.matches("E-MTAB.*|E-ERAD.*|E-GEUV.*")) {
            externalResourcesLinks.addAll(linkToArrayExpress.get(experiment));
        }

        return externalResourcesLinks.build();
    }

    private ImmutableList.Builder<ExternallyAvailableContent> externalResourceLinks(Experiment<?> experiment) {
        ImmutableList.Builder<ExternallyAvailableContent> otherExternalResourceLinks = ImmutableList.builder();

        Map<String, List<String>> resourceList = experiment.getSecondaryAccessions().stream()
                .collect(Collectors.groupingBy(accession -> {
                    if (accession.matches("GSE.*")) return "GEO";
                    if (accession.matches("EGA.*")) return "EGA";
                    if (accession.matches("PDX.*")) return "PRIDE";
                    if (accession.matches("ERP.*|SRP.*|DRP.*|PRJEB.*|PRJNA.*|PRJDB.*")) return "ENA";
                    return "OTHER";
                }));

        resourceList.entrySet().removeIf(entry -> {
            String resource = entry.getKey();
            var accessions = entry.getValue().stream()
                    .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

            switch (resource) {
                case "GEO":
                    otherExternalResourceLinks.addAll(linkToGeo.get(experiment));
                    break;
                case "EGA":
                    otherExternalResourceLinks.addAll(linkToEga.get(experiment));
                    break;
                case "ENA":
                    otherExternalResourceLinks.addAll(linkToEna.get(experiment));
                    break;
                case "PRIDE":
                    otherExternalResourceLinks.addAll(linkToPride.get(experiment));
                    break;
                case "OTHER":
                    // Remove this entry by returning true
                    return true;
            }

            // Update the entry's value
            entry.setValue(accessions);
            return false;
        });

        return otherExternalResourceLinks;
    }

}
