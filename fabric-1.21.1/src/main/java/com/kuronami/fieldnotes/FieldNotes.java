package com.kuronami.fieldnotes;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Static constants shared across Field Notes classes (MODID, LOGGER). */
public final class FieldNotes {
    public static final String MODID = "fieldnotes";
    public static final Logger LOGGER = LogUtils.getLogger();

    private FieldNotes() {}
}
