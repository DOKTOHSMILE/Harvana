package com.harvana.app.wallet;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A saved payment method a person can receive fiat through when
 * selling HARV on the P2P marketplace. Deliberately maps onto the same
 * three string fields P2PTradeRouter.createListing() already signs
 * and sends to the node (bankName/accountNumber/accountName) — adding
 * a real "type" field to the signed transaction format would mean
 * touching the node's protocol, which is out of scope for what's
 * fundamentally a client-side convenience layer. For BANK, primaryField
 * is the bank name; for MOBILE_MONEY, it's the provider name (e.g.
 * "MTN Mobile Money"). accountIdentifier is an account number or a
 * phone number depending on type. Both ultimately submit as
 * bankName/accountNumber/accountName with no protocol changes needed.
 */
public class PaymentMethod {

	public enum Type { BANK, MOBILE_MONEY }

	public String id;
	public Type type;
	public String label;              // user's own nickname, e.g. "GTBank - Personal"
	public String primaryField;       // bank name, or mobile money provider name
	public String accountIdentifier;  // account number, or phone number
	public String accountName;        // account holder's name
	public boolean isDefault;
	/** Where the account is (ISO code, "OTHER" for elsewhere); "" on methods saved before countries existed (Nigeria). */
	public String country = "";
	/** The country's extra fields as typed (sortCode, routing, iban, bic, ifsc, bsb…). See banks/AccountRules. */
	public java.util.Map<String, String> details = new java.util.LinkedHashMap<>();
	/** The account number or IBAN as typed, before the details are folded into accountIdentifier. */
	public String accountMain = "";

	public PaymentMethod() {}

	public PaymentMethod(String id, Type type, String label, String primaryField,
	String accountIdentifier, String accountName, boolean isDefault) {
		this.id = id;
		this.type = type;
		this.label = label;
		this.primaryField = primaryField;
		this.accountIdentifier = accountIdentifier;
		this.accountName = accountName;
		this.isDefault = isDefault;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject o = new JSONObject();
		o.put("id", id);
		o.put("type", type.name());
		o.put("label", label);
		o.put("primaryField", primaryField);
		o.put("accountIdentifier", accountIdentifier);
		o.put("accountName", accountName);
		o.put("isDefault", isDefault);
		o.put("country", country == null ? "" : country);
		o.put("accountMain", accountMain == null ? "" : accountMain);
		JSONObject d = new JSONObject();
		if (details != null) for (java.util.Map.Entry<String, String> e : details.entrySet()) d.put(e.getKey(), e.getValue());
		o.put("details", d);
		return o;
	}

	public static PaymentMethod fromJson(JSONObject o) throws JSONException {
		PaymentMethod pm = new PaymentMethod();
		pm.id = o.getString("id");
		pm.type = Type.valueOf(o.getString("type"));
		pm.label = o.getString("label");
		pm.primaryField = o.getString("primaryField");
		pm.accountIdentifier = o.getString("accountIdentifier");
		pm.accountName = o.getString("accountName");
		pm.isDefault = o.optBoolean("isDefault", false);
		pm.country = o.optString("country", "");
		pm.accountMain = o.optString("accountMain", "");
		JSONObject d = o.optJSONObject("details");
		if (d != null) {
			java.util.Iterator<String> keys = d.keys();
			while (keys.hasNext()) { String k = keys.next(); pm.details.put(k, d.optString(k)); }
		}
		return pm;
	}

	/** The exact display line CreateP2pListingActivity's review step
*  already shows for bank details, extended to also read sensibly
*  for a mobile money method. */
	public String displaySummary() {
		return primaryField + " · " + accountIdentifier + " · " + accountName;
	}
}
