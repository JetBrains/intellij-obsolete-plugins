package client;

import com.google.gwt.i18n.client.Messages;

public interface PluralMessages extends Messages {
  String friends(@PluralCount int number);
}