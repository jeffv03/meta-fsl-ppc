DESCRIPTION = "U-boot bootloader"
HOMEPAGE = "http://u-boot.sf.net"
SECTION = "bootloaders"
PROVIDES = "virtual/bootloader"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=1707d6db1d42237583f50183a5651ecb"

PR = "r26"
INHIBIT_DEFAULT_DEPS = "1"
DEPENDS = "boot-format-native virtual/${TARGET_PREFIX}gcc libgcc"

inherit deploy

SRCREV = "6d1aef1c02ba9472215234696faa8ce8745a40a9"
SRC_URI = "git://git.freescale.com/ppc/sdk/u-boot.git"

SRCREV_e6500 = "59852d03867108217fe88e3bfc3e1e9cedfe63c5"
SRCREV_e6500-64b = "59852d03867108217fe88e3bfc3e1e9cedfe63c5"
SRC_URI_e6500 = "git://git.denx.de/u-boot.git"
SRC_URI_e6500-64b = "git://git.denx.de/u-boot.git"

python () {
	ml = d.getVar("MULTILIB_VARIANTS", True)
	arch = d.getVar("OVERRIDES", True)

	if ("e5500-64b:" in arch or "e6500-64b:" in arch) and not "lib32" in ml:
		raise bb.parse.SkipPackage("Building the u-boot for this arch requires multilib to be enabled")
}

DEPENDS_append_e5500-64b = " lib32-gcc-cross lib32-libgcc"
PATH_append_e5500-64b = ":${STAGING_BINDIR_NATIVE}/${DEFAULTTUNE_virtclass-multilib-lib32}${TARGET_VENDOR_virtclass-multilib-lib32}-${HOST_OS}/"
TOOLCHAIN_OPTIONS_append_e5500-64b = "/../lib32-${MACHINE}"
TARGET_VENDOR_virtclass-multilib-lib32 ?= "-${DISTRO}mllib32"
WRAP_TARGET_PREFIX_e5500-64b = "powerpc${TARGET_VENDOR_virtclass-multilib-lib32}-${HOST_OS}-"

DEPENDS_append_e6500-64b = " lib32-gcc-cross lib32-libgcc"
PATH_append_e6500-64b = ":${STAGING_BINDIR_NATIVE}/${DEFAULTTUNE_virtclass-multilib-lib32}${TARGET_VENDOR_virtclass-multilib-lib32}-${HOST_OS}/"
TOOLCHAIN_OPTIONS_append_e6500-64b = "/../lib32-${MACHINE}"
TARGET_VENDOR_virtclass-multilib-lib32 ?= "-${DISTRO}mllib32"
WRAP_TARGET_PREFIX_e6500-64b = "powerpc${TARGET_VENDOR_virtclass-multilib-lib32}-${HOST_OS}-"

WRAP_TARGET_PREFIX = "${TARGET_PREFIX}"
EXTRA_OEMAKE = 'CROSS_COMPILE=${WRAP_TARGET_PREFIX} CC="${WRAP_TARGET_PREFIX}gcc ${TOOLCHAIN_OPTIONS}"'

PACKAGE_ARCH = "${MACHINE_ARCH}"

USRC ?= ""
S = '${@base_conditional("USRC", "", "${WORKDIR}/git", "${USRC}", d)}'

do_compile () {
	unset LDFLAGS
	unset CFLAGS
	unset CPPFLAGS

	if [ "x${UBOOT_MACHINES}" == "x" ]; then
		UBOOT_MACHINES=${UBOOT_MACHINE}
	fi

	for board in ${UBOOT_MACHINES}; do
		oe_runmake O=${board} distclean
		oe_runmake O=${board} ${board}
		oe_runmake O=${board} all

		case "${board}" in
		*SDCARD*)	UBOOT_TARGET="u-boot-sd";;
		*SPIFLASH*)	UBOOT_TARGET="u-boot-spi";;
		*NAND*)		UBOOT_TARGET="u-boot-nand";;
		*)		UBOOT_TARGET="";;
		esac

		if [ "x${UBOOT_TARGET}" != "x" ]; then
			if [ "${MACHINE_ARCH}" == "p1023rds" ] || \
				[ "${MACHINE_ARCH}" == "p2041rdb" ] || \
	                        [ "${MACHINE_ARCH}" == "p3041ds" ] || \
        	                [ "${MACHINE_ARCH}" == "p4080ds" ] || \
                	        [ "${MACHINE_ARCH}" == "p5020ds" ] || \
                	        [ "${UBOOT_TARGET}" == "u-boot-sd" ] || \
				[ "${UBOOT_TARGET}" == "u-boot-nand" ]; then
				cp ${S}/${board}/u-boot.bin  ${S}/${board}/${UBOOT_TARGET}.bin
			else
				${STAGING_BINDIR_NATIVE}/boot_format \
					${STAGING_DATADIR_NATIVE}/boot_format/${BOOTFORMAT_CONFIG} \
					${S}/${board}/u-boot.bin -spi ${S}/${board}/${UBOOT_TARGET}.bin
			fi 
		fi
	done
}

do_install(){
	if [ "x${UBOOT_MACHINES}" == "x" ]; then
		UBOOT_MACHINES=${UBOOT_MACHINE}
	fi

	for board in ${UBOOT_MACHINES}; do
		case "${board}" in
		*SDCARD*)	UBOOT_TARGET="u-boot-sd";;
		*SPIFLASH*)	UBOOT_TARGET="u-boot-spi";;
		*NAND*)		UBOOT_TARGET="u-boot-nand";;
		*)		UBOOT_TARGET="u-boot";;
		esac

		if [ -f ${S}/${board}/${UBOOT_TARGET}.bin ]; then
			mkdir -p ${D}/boot/
			install ${S}/${board}/${UBOOT_TARGET}.bin ${D}/boot/${UBOOT_TARGET}-${board}-${PV}-${PR}.bin
			ln -sf ${UBOOT_TARGET}-${board}-${PV}-${PR}.bin ${D}/boot/${UBOOT_TARGET}.bin
		fi
	done
}

do_deploy(){
	if [ "x${UBOOT_MACHINES}" == "x" ]; then
		UBOOT_MACHINES=${UBOOT_MACHINE}
	fi

	for board in ${UBOOT_MACHINES}; do
		case "${board}" in
		*SDCARD*)	UBOOT_TARGET="u-boot-sd";;
		*SPIFLASH*)	UBOOT_TARGET="u-boot-spi";;
		*NAND*)		UBOOT_TARGET="u-boot-nand";;
		*)		UBOOT_TARGET="u-boot";;
		esac

	        if [ -f ${S}/${board}/${UBOOT_TARGET}.bin ]; then
			mkdir -p ${DEPLOYDIR}
			install ${S}/${board}/${UBOOT_TARGET}.bin ${DEPLOYDIR}/${UBOOT_TARGET}-${board}-${PV}-${PR}.bin

			cd ${DEPLOYDIR}
			rm -f ${UBOOT_TARGET}-${board}.bin
                        ln -sf ${UBOOT_TARGET}-${board}-${PV}-${PR}.bin ${UBOOT_TARGET}-${board}.bin
		fi
	done
}
addtask deploy after do_install

PACKAGES += "${PN}-images"
FILES_${PN}-images += "/boot"

ALLOW_EMPTY = "1"
