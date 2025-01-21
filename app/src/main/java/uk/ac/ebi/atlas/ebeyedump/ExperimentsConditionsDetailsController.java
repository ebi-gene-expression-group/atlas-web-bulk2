package uk.ac.ebi.atlas.ebeyedump;

import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesign;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

import static java.lang.String.join;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE_DIA;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;

@Controller
@Scope("request")
public class ExperimentsConditionsDetailsController {
    private final ExperimentTrader experimentTrader;

    public ExperimentsConditionsDetailsController(ExperimentTrader experimentTrader) {
        this.experimentTrader = experimentTrader;
    }

    @GetMapping("/api/assaygroupsdetails.tsv")
    public void generateTsvFormatBaseline(HttpServletResponse response) {
        writeTsvLinesToResponse(
                response,
                "assaygroupsdetails.tsv",
                experiment -> new BaselineExperimentAssayGroupsLines(
                        (BaselineExperiment) experiment, getExperimentDesign(experiment.getAccession())),
                RNASEQ_MRNA_BASELINE,
                PROTEOMICS_BASELINE,
                PROTEOMICS_BASELINE_DIA);
    }

    @GetMapping("/api/contrastdetails.tsv")
    public void generateTsvFormatDifferential(HttpServletResponse response) {
        writeTsvLinesToResponse(
                response,
                "contrastdetails.tsv",
                experiment -> new DifferentialExperimentContrastLines(
                        (DifferentialExperiment) experiment, getExperimentDesign(experiment.getAccession())),
                MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL,
                MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL,
                MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL,
                RNASEQ_MRNA_DIFFERENTIAL);
    }

    private ExperimentDesign getExperimentDesign(String experimentAccession) {
        return experimentTrader.getExperimentDesign(experimentAccession);
    }

    private void writeTsvLinesToResponse(HttpServletResponse response,
                                         String fileName,
                                         Function<Experiment<?>, Iterable<String[]>> linesIteratorProducer,
                                         ExperimentType... experimentTypes) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName);
        response.setContentType("text/tab-separated-values");

        try {
            for (Experiment<?> experiment : experimentTrader.getPublicExperiments(experimentTypes)) {
                for (String[] line : linesIteratorProducer.apply(experiment)) {
                    response.getWriter().write(join("\t", line) + "\n");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
