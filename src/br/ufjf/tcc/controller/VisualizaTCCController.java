package br.ufjf.tcc.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Window;

import br.ufjf.tcc.business.PerguntaBusiness;
import br.ufjf.tcc.business.QuestionarioBusiness;
import br.ufjf.tcc.business.RespostaBusiness;
import br.ufjf.tcc.business.TCCBusiness;
import br.ufjf.tcc.library.SessionManager;
import br.ufjf.tcc.model.Participacao;
import br.ufjf.tcc.model.Pergunta;
import br.ufjf.tcc.model.Resposta;
import br.ufjf.tcc.model.TCC;

public class VisualizaTCCController extends CommonsController {
	private TCC tcc;
	private boolean userCanAnswer = true;
	private boolean tccNeedsAnswer = true;
	private List<Resposta> answers = new ArrayList<Resposta>();

	@Init
	public void init() {
		this.tcc = (TCC) SessionManager.getAttribute("tcc");

		tccNeedsAnswer = tcc.getConceitoFinal() == -1;
		if (tccNeedsAnswer) {
			for (Participacao p : tcc.getParticipacoes())
				if (p.getProfessor().getIdUsuario() == getUsuario()
						.getIdUsuario()) {
					userCanAnswer = true;
					break;
				}
			if (userCanAnswer) {
				List<Pergunta> questions = new PerguntaBusiness()
						.getQuestionsByQuestionary(new QuestionarioBusiness()
								.getCurrentQuestionaryByCurso(tcc.getAluno()
										.getCurso()));

				for (Pergunta question : questions) {
					Resposta answer = new Resposta();
					answer.setPergunta(question);
					answers.add(answer);
				}
			}
		}

	}

	public TCC getTcc() {
		return tcc;
	}

	public void setTcc(TCC tcc) {
		this.tcc = tcc;
	}

	public boolean isUserCanAnswer() {
		return userCanAnswer;
	}

	public void setUserCanAnswer(boolean userCanAnswer) {
		this.userCanAnswer = userCanAnswer;
	}

	public boolean isTccNeedsAnswer() {
		return tccNeedsAnswer;
	}

	public void setTccNeedsAnswer(boolean tccNeedsAnswer) {
		this.tccNeedsAnswer = tccNeedsAnswer;
	}
	
	public List<Resposta> getAnswers() {
		return answers;
	}

	@Command
	public void showTCC(@BindingParam("iframe") Iframe report) {
		InputStream is = Sessions.getCurrent().getWebApp()
				.getResourceAsStream("files/" + tcc.getArquivoTCCFinal());

		final AMedia amedia = new AMedia("PDFReference16.pdf", "pdf",
				"application/pdf", is);
		report.setContent(amedia);
	}

	@Command
	public void getTccYear(@BindingParam("lbl") Label lbl) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(tcc.getDataEnvioFinal().getTime());
		lbl.setValue("" + cal.get(Calendar.YEAR));
	}

	@Command
	public void downloadPDF() {
		InputStream is = Sessions.getCurrent().getWebApp()
				.getResourceAsStream("files/" + tcc.getArquivoTCCFinal());
		Filedownload.save(is, "application/pdf", tcc.getNomeTCC() + ".pdf");
	}

	@Command
	public void downloadExtra() {
		if (tcc.getArquivoExtraTCCFinal() != null
				&& tcc.getArquivoExtraTCCFinal() != "") {
			InputStream is = Sessions
					.getCurrent()
					.getWebApp()
					.getResourceAsStream(
							"files/" + tcc.getArquivoExtraTCCFinal());
			Filedownload.save(is, "application/x-rar-compressed",
					tcc.getNomeTCC() + ".rar");
		}
	}

	@Command
	public void logout() {
		new MenuController().sair();
	}
	
	@Command
	public void submit(@BindingParam("window") Window window) {
		RespostaBusiness respostaBusiness = new RespostaBusiness();
		float sum = 0;
		for (Resposta a : answers) {
			if (respostaBusiness.validate(a)) {
				sum += a.getResposta();
				respostaBusiness.save(a);
			} else {
				String errorMessage = "";
				for (String error : respostaBusiness.errors)
					errorMessage += error;
				Messagebox.show(errorMessage, "Dados insuficientes / inválidos",
						Messagebox.OK, Messagebox.ERROR);
				clearErrors(respostaBusiness);
				return;
			}
		}

		tcc.setConceitoFinal(sum);
		new TCCBusiness().edit(tcc);

		Messagebox.show("Conceito final: " + sum);
		window.detach();
	}
	
	public void clearErrors(RespostaBusiness respostaBusiness) {
		respostaBusiness.errors.clear();
	}
}