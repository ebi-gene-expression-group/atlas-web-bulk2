package uk.ac.ebi.atlas.trader;

import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.sample.Contrast;

@Component
public class ContrastTrader {
    private ExperimentTrader experimentTrader;

    public ContrastTrader(ExperimentTrader experimentTrader) {
        this.experimentTrader = experimentTrader;
    }

    public Contrast getContrast(String experimentAccession, String contrastId) {
        Experiment experiment = experimentTrader.getPublicExperiment(experimentAccession);
        return ((DifferentialExperiment) experiment).getDataColumnDescriptor(contrastId);
    }
}
