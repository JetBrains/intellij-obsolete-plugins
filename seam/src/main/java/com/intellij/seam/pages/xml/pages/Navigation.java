package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Navigation extends TaskOwner, RenderAndRedirectOwner, SeamPagesDomElement {

	@NotNull
	GenericAttributeValue<String> getFromAction();

	@NotNull
	GenericAttributeValue<String> getEvaluate();

	@NotNull
	List<Rule> getRules();
	Rule addRule();


	@NotNull
	List<Out> getOuts();
	Out addOut();

	@NotNull
	RaiseEvent getRaiseEvent();

	@NotNull
	BeginConversation getBeginConversation();

	@NotNull
	EndConversation getEndConversation();

	@NotNull
	CreateProcess getCreateProcess();

	@NotNull
	ResumeProcess getResumeProcess();
}
