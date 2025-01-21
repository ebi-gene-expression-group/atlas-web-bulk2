package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experiments.ExperimentJsonSerializer;
import uk.ac.ebi.atlas.model.card.CardModel;
import uk.ac.ebi.atlas.model.card.CardModelAdapter;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.Optional;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static uk.ac.ebi.atlas.model.card.CardIconType.IMAGE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE_DIA;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;
import static uk.ac.ebi.atlas.utils.UrlHelpers.getCustomUrl;
import static uk.ac.ebi.atlas.utils.UrlHelpers.getExperimentLink;
import static uk.ac.ebi.atlas.utils.UrlHelpers.getExperimentSetLink;
import static uk.ac.ebi.atlas.utils.UrlHelpers.getExperimentsSummaryImageUrl;
import static uk.ac.ebi.atlas.utils.UrlHelpers.getLinkWithEmptyLabel;

@RestController
public class JsonExperimentsSummaryController extends JsonExceptionHandlingController {
    private static final String COVID19_KEYWORDS = "sars contributing coronavirus rsv ifn-alpha infection-related";

    private final LatestExperimentsService latestExperimentsService;

    public JsonExperimentsSummaryController(LatestExperimentsDao latestExperimentsDao,
                                            ExperimentTrader experimentTrader,
                                            ExperimentJsonSerializer experimentJsonSerializer) {
        this.latestExperimentsService =
                new LatestExperimentsService(
                        latestExperimentsDao,
                        experimentTrader,
                        ImmutableSet.of(
                                MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL,
                                MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL,
                                MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL,
                                RNASEQ_MRNA_DIFFERENTIAL,
                                PROTEOMICS_BASELINE,
                                PROTEOMICS_BASELINE_DIA,
                                RNASEQ_MRNA_BASELINE),
                        experimentJsonSerializer);
    }

    @GetMapping(value = "/json/experiments-summary",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getLatestExperiments() {
        return GSON.toJson(
                ImmutableMap.of(
                        "latestExperiments",
                        latestExperimentsService.fetchLatestExperimentsAttributes().get("latestExperiments"),
                        "featuredExperiments",
                        featuredExperimentsCards().stream()
                                .map(CardModelAdapter::serialize)
                                .collect(toImmutableList())));
    }



    private static ImmutableList<CardModel> featuredExperimentsCards() {
        return ImmutableList.of(
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("covid19-dp"),
                        getExperimentSetLink(COVID19_KEYWORDS),
                        ImmutableList.of(
                                Pair.of("Human experiments", Optional.of(getCustomUrl("/experiments?species=homo+sapiens&experimentDescription=" + COVID19_KEYWORDS))),
                                Pair.of("Mouse experiments", Optional.of(getCustomUrl("/experiments?species=mus+musculus&experimentDescription="+ COVID19_KEYWORDS))))
                ),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("encode"),
                        getExperimentSetLink("ENCODE"),
                        ImmutableList.of(
                                getExperimentLink("Human tissues", "E-MTAB-4344"),
                                getExperimentLink("Human cells", "E-GEOD-26284"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("blueprint"),
                        getExperimentSetLink("BLUEPRINT"),
                        ImmutableList.of(
                                getExperimentLink("Plasma cells of tonsil", "E-MTAB-4754"),
                                getExperimentLink("Rare types of haemopoetic cells", "E-MTAB-3819"),
                                getExperimentLink("Common types of haemopoetic cells", "E-MTAB-3827"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("fantom"),
                        getExperimentSetLink("FANTOM5"),
                        ImmutableList.of(
                                getExperimentLink("Mouse cells", "E-MTAB-3578"),
                                getExperimentLink("Mouse tissues", "E-MTAB-3579"),
                                getExperimentLink("Human tissues", "E-MTAB-3358"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("human_protein_atlas"),
                        getExperimentLink("E-PROT-3"),
                        ImmutableList.of(
                                getExperimentLink("Human tissues", "E-PROT-3"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("ccle"),
                        getExperimentLink("E-MTAB-2770"),
                        ImmutableList.of(
                                getExperimentLink("Cancer Cell Line Encyclopedia", "E-MTAB-2770"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("hipsci"),
                        getExperimentSetLink("HipSci"),
                        ImmutableList.of(
                                getExperimentLink("Proteomics – Cell lines", "E-PROT-5"),
                                getExperimentLink("RNA – Cell lines", "E-MTAB-4748"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("gtex"),
                        getExperimentLink("E-GTEX-8"),
                        ImmutableList.of(
                                getExperimentLink("Human tissues", "E-GTEX-8"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("pcawg"),
                        getExperimentSetLink("Pan-Cancer"),
                        ImmutableList.of(
                                getExperimentLink("PCAWG by disease", "E-MTAB-5200"),
                                getExperimentLink("PCAWG by individual", "E-MTAB-5423"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("wtsi_mgh_cancerrxgene"),
                        getExperimentLink("E-MTAB-3983"),
                        ImmutableList.of(
                                getExperimentLink("Genomics of Drug Sensitivity in Cancer Project – Cell lines", "E-MTAB-3983"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("hdbr"),
                        getExperimentLink("E-MTAB-4840"),
                        ImmutableList.of(
                                getExperimentLink("Prenatal brain development", "E-MTAB-4840"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("edtl"),
                        getLinkWithEmptyLabel(getCustomUrl("/edtl/experiments")),
                        ImmutableList.of(
                                Pair.of("European Diagnostic Transcriptomic Library", Optional.of(getCustomUrl("/edtl/experiments"))))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("baseline"),
                        getLinkWithEmptyLabel(getCustomUrl("/baseline/experiments")),
                        ImmutableList.of(
                                Pair.of("Baseline experiments", Optional.of(getCustomUrl("/baseline/experiments"))))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("gramene"),
                        getLinkWithEmptyLabel(getCustomUrl("/plant/experiments")),
                        ImmutableList.of(
                                Pair.of("Plant experiments", Optional.of(getCustomUrl("/plant/experiments"))))));
    }
}
