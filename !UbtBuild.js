// Build Script: See documentation for ABL Universal Build Tool (UBT).
"use strict";

let     log

// *********************************************************************************************************************
// SETUP AND TEARDOWN
// *********************************************************************************************************************

export async function setup(ctx,ubt) {
    log = ubt.log;

    ctx.java.tgtFolder = ubt.fsInfo(ctx.prjFolder);
    }

export async function teardown(ctx,ubt) {
    }

// *********************************************************************************************************************
// BUILD ACTIONS
// *********************************************************************************************************************

export async function buildFile(ctx,ubt) {
    if(ctx.file.nameExt===".java") {
        await ubt.compileJava(ctx.file);
        }
    }

export async function buildFolder(ctx,ubt) {
    await ubt.deleteFiles(ubt.findFiles(ctx.folder,"*.class"));
    await ubt.compileJava(ubt.findFiles(ctx.folder,"*.java" ));
    }

export async function buildProject(ctx,ubt) {
    ubt.heading2("Build ActiveLauncher");
    ubt.deleteFiles(ubt.findFiles(ctx.prjFolder,"*.class"));
    await ubt.compileJava(ubt.findFiles(ctx.prjFolder,"*.java"));
    }

// *********************************************************************************************************************
