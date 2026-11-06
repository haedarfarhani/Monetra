package ir.heydar.monetra.feature.smsimport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import ir.heydar.monetra.feature.smsimport.parser.BankMessageParser
import ir.heydar.monetra.feature.smsimport.parser.GenericBankParser
import ir.heydar.monetra.feature.smsimport.parser.MellatBankParser
import ir.heydar.monetra.feature.smsimport.parser.MelliBankParser
import ir.heydar.monetra.feature.smsimport.parser.ParsianBankParser
import ir.heydar.monetra.feature.smsimport.parser.PasargadBankParser
import ir.heydar.monetra.feature.smsimport.parser.SaderatBankParser
import ir.heydar.monetra.feature.smsimport.parser.SamanBankParser
import ir.heydar.monetra.feature.smsimport.parser.SepahBankParser
import ir.heydar.monetra.feature.smsimport.parser.TejaratBankParser

@Module
@InstallIn(SingletonComponent::class)
abstract class SmsParserModule {

    @Binds
    @IntoSet
    abstract fun bindMelliParser(impl: MelliBankParser): BankMessageParser

    @Binds
    @IntoSet
    abstract fun bindMellatParser(impl: MellatBankParser): BankMessageParser

    @Binds
    @IntoSet
    abstract fun bindSaderatParser(impl: SaderatBankParser): BankMessageParser

    @Binds
    @IntoSet
    abstract fun bindTejaratParser(impl: TejaratBankParser): BankMessageParser

    @Binds
    @IntoSet
    abstract fun bindSepahParser(impl: SepahBankParser): BankMessageParser

    @Binds
    @IntoSet
    abstract fun bindPasargadParser(impl: PasargadBankParser): BankMessageParser

    @Binds
    @IntoSet
    abstract fun bindSamanParser(impl: SamanBankParser): BankMessageParser

    @Binds
    @IntoSet
    abstract fun bindParsianParser(impl: ParsianBankParser): BankMessageParser

    @Binds
    @IntoSet
    abstract fun bindGenericParser(impl: GenericBankParser): BankMessageParser
}
