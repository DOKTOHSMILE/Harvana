package com.harvanna.app.wallet;

import android.graphics.Color;
import com.harvanna.app.R;

public enum CardTemplate {
	
	// ── Legacy Gradient / Solid Color Cards ──
	
	CLASSIC(
	"Harvanna",
	new int[]{Color.parseColor("#111827"), Color.parseColor("#1F2937")},
	Color.WHITE,
	Color.parseColor("#9CA3AF"),
	Color.parseColor("#FF9030"),
	false,
	0
	),
	
	OBSIDIAN_BLACK(
	"Obsidian",
	new int[]{Color.parseColor("#0A0A0A"), Color.parseColor("#1C1C1E"), Color.parseColor("#0A0A0A")},
	Color.parseColor("#F2F2F2"),
	Color.parseColor("#9A9A9A"),
	Color.parseColor("#00D9A5"),
	true,
	0
	),
	
	GOLD_ELITE(
	"Gold Elite",
	new int[]{
		Color.parseColor("#BF953F"), Color.parseColor("#FCF6BA"),
		Color.parseColor("#B38728"), Color.parseColor("#FBF5B7"),
		Color.parseColor("#AA771C")
	},
	Color.parseColor("#1A1200"),
	Color.parseColor("#4A3A10"),
	Color.parseColor("#1A1200"),
	true,
	0
	),
	
	// ── New Thematic Cards using Image Drawables - AMBER FIX ──
	
	OASIS(
	"Oasis", null,
	Color.parseColor("#7C2D12"), // heading - darker burnt amber
	Color.parseColor("#92400E"), // body
	Color.parseColor("#B45309"), // keep your QR border color as accent
	false,
	R.drawable.bg_oasis
	),
	
	NODE(
	"Node",
	null,
	Color.parseColor("#FFB000"), // bright amber - high contrast on dark node bg
	Color.parseColor("#FFD166"),
	Color.parseColor("#FF8C00"),
	true, // metallic sheen helps amber pop
	R.drawable.bg_node
	),
	
	CYBER(
	"Cyber",
	null,
	Color.parseColor("#FFC107"), // amber for better visuals on dark cyber
	Color.parseColor("#FFE082"),
	Color.parseColor("#FF9800"),
	true,
	R.drawable.bg_cyber
	),
	
	MINT(
	"Mint",
	null,
	Color.parseColor("#78350F"), // dark amber - readable on light mint
	Color.parseColor("#92400E"),
	Color.parseColor("#B45309"),
	false,
	R.drawable.bg_mint
	);
	
	public final String displayName;
	public final int[] gradientColors;
	public final int textPrimary;
	public final int textSecondary;
	public final int accent;
	public final boolean metallic;
	public final int backgroundDrawableId;
	
	CardTemplate(String displayName, int[] gradientColors, int textPrimary,
	int textSecondary, int accent, boolean metallic, int backgroundDrawableId) {
		this.displayName = displayName;
		this.gradientColors = gradientColors;
		this.textPrimary = textPrimary;
		this.textSecondary = textSecondary;
		this.accent = accent;
		this.metallic = metallic;
		this.backgroundDrawableId = backgroundDrawableId;
	}
}
