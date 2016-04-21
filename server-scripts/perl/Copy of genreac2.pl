#!/usr/bin/perl

#-- Combina modelos metabolicos
#-- Esta version incluye la busqueda y ajuste de reacciones de biomasa

#-- Javier Tamames, Mayo 2012

use Data::Dumper;

$| = 1;

$minmediasw   = 0;    #-- A uno, ajusta el medio a la composicion de los medios minimos de cada componente
$newobjective = 0;    #-- A uno, elimina las reacciones objetivo iniciales y crea una nueva

for my $models (@ARGV) {
	if ( $models =~ /^-minmedia$/ ) {
		$minmediasw = 1;
	}
	elsif ( $models =~ /^-newobj$/ ) {
		$newobjective = 1;
	}
	else {
		$nummodel++;
		$namemodel .= "$nummodel: $models; ";
		extractReactions( $models, $nummodel );    #-- Lectura de modelos y almacenamiento de componentes
		if ($minmediasw) { minmedia( $models, $nummodel ); }    #-- Lectura de medios minimos (ya generados por minmedia.m)
	}
}

metab();                                                    #-- Normalizacion de compuestos
reacs();                                                    #-- Normalizacion de reacciones

#if($newobjective) { objective($nummodel); }    #-- Aniade una nueva reaccion objetivo
objective($nummodel);
writeModel();                                               #-- Salida

sub extractReactions {
	open( IN, $_[0] );
	$model          = $_[1];
	$reactionId     = "";
	$reactionName   = "";
	$reactorproduct = "";
	while (<IN>) {
		chomp;
		next if !$_;
		my $line = $_;
		$line =~ s/\t//g;
		$line =~ s/\s+/ /g;

		if ( $line =~ /^[\s*]?<compartment\s+id=\"([^"]*)\"(?=.*outside=\"([^"]*)\")?(?=.*name=\"([^"]*)\")?/ ) {

			#my($ffw,$ssw)=split(/\_/,$1);
			#if($ssw) { $iwd=$ssw; } else { $iwd=$ffw; }
			$iwd = $1;
			$iwd =~ s/\_//g;
			$abcomp = substr( $iwd, 0, 1 );
			$abcomp =~ tr/A-Z/a-z/;
			$outcomp = substr( $2, 0, 1 );
			$outcomp =~ tr/A-Z/a-z/;
			if ( $abcomp ne "e" ) {
				$incm = "$abcomp\_$model";
				if   ( $outcomp ne "e" ) { $outcm = "$outcomp\_$model"; }
				else                     { $outcm = $outcomp; }
				$outcompart{$incm} = $outcm;
			}
		}

		if ( $line =~ /^[\s*]?\<species id=\"([^"]*)\"(?=.*name=\"([^"]*)\")(?=.*compartment=\"([^"]*)\")(?=.*charge=\"([^"]*)\")?(?=.*boundaryCondition=\"([^"]*)\")?/ ) {
			$coid    = $1;
			$coname  = $2;
			$compart = $3;
			$tcharge = $4;
			$tboun   = $5;

			# if($model>0) { print "$_\n"; }
			#Sacamos formula quimica del nombre
			my @cp_parts = split( /\_/, $coname );
			my $compoundFormula = "";
			foreach my $cp_part (@cp_parts) {
				$compoundFormula = $cp_part;
			}

			#  my($ffw,$ssw)=split(/\_/,$compart);
			#  if($ssw) { $iwd=$ssw; } else { $iwd=$ffw; }
			$iwd = $compart;
			$iwd =~ s/\_//g;

			$abcompart = substr( $iwd, 0, 1 );
			$abcompart =~ tr/A-Z/a-z/;

			# my $crea =$dbh->do("INSERT INTO compounds values(\"$1\",\"$compoundFormula\",\"$3\",\"$model\",\"$2\",\"$4\",\"$5\");");
			$compounds{$coid}{$model}{formula}     = $compoundFormula;
			$compounds{$coid}{$model}{compartment} = $abcompart;
			$compounds{$coid}{$model}{name}        = $coname;
			$compounds{$coid}{$model}{charge}      = $tcharge;
			$compounds{$coid}{$model}{boundary}    = $tboun;

			# print "*$coid*$model*$coname*$compounds{$1}{$model}{compartment}*\n";
		}
		elsif ( $line =~ /^[\s*]?\<reaction id=\"([^"]*)\"\s+name=\"([^"]*)\"(?=.*reversible=\"([^"]*)\")?(?=.*metaid=\"([^"]*)\")?/ ) {
			$reactionId                                    = $1;
			$reactionName                                  = $2;
			$reactions{$reactionId}{$model}{reactionname}  = $reactionName;
			$reactions{$reactionId}{$model}{reversibility} = $3;
		}
		elsif ( $line =~ /^[\s*]?\<listOfReactants/ ) {
			$reactorproduct = "react";
		}
		elsif ( $line =~ /^[\s*]?\<listOfProducts/ ) {
			$reactorproduct = "product";
		}
		elsif ( $line =~ /^[\s*]?<parameter\s+id=\"([^"]*)\"\s+value=\"([^"]*)\"/ ) {
			my $parameter_value = $2;
			if    ( $1 =~ /LOWER_BOUND/ ) { $reactions{$reactionId}{$model}{lowerbound} = $parameter_value; }
			elsif ( $1 =~ /UPPER_BOUND/ ) { $reactions{$reactionId}{$model}{upperbound} = $parameter_value; }
			elsif ( $1 =~ /OBJECTIVE_COEFFICIENT/ ) {
				$reactions{$reactionId}{$model}{objectcoef} = $parameter_value;
				if ( $parameter_value > 0 ) { $objectivereact{$model}{$reactionId} = $parameter_value; }
			}
			elsif ( $1 =~ /FLUX_VALUE/ ) {
				$reactions{$reactionId}{$model}{fluxvalue} = $parameter_value;
			}
		}
		elsif ( $line =~ /^[\s*]?\<speciesReference\s+species=\"([^"]*)\"(?=.*stoichiometry=\"([^"]*)\")?/ ) {

			$compreactions{$reactionId}{$model}{$reactorproduct}{$1} = $2;
		}
		elsif ( $line =~ /^[\s*]?\<\/reaction/ ) {
			if ( $objectivereact{$model}{$reactionId} ) {    #-- Vamos a buscar si hay termino de biomasa
				foreach my $prods ( keys %{ $compreactions{$reactionId}{$model}{product} } ) {
					$comname = $compounds{$prods}{$model}{name};
					if ( $comname =~ /biomass/i ) { $biomass{$model} = $comname; }
				}
				if ( !$biomass{$model} ) {                     #-- Si no lo hay, se aniade
					$bterm                                               = "biomass$model\_c";
					$biomass{$model}                                     = $bterm;
					$compreactions{$reactionId}{$model}{product}{$bterm} = 1;
					$compounds{$bterm}{$model}{compartment}              = "c";
					$compounds{$bterm}{$model}{name}                     = $bterm;
					$compounds{$bterm}{$model}{charge}                   = 0;
					$compounds{$bterm}{$model}{boundary}                 = "false";
				}
			}
			$reactionId     = "";
			$reactionName   = "";
			$reactorproduct = "";
		}

	}

}

sub metab {
	foreach my $comps ( sort keys %compounds ) {
		foreach my $inmodel ( sort keys %{ $compounds{$comps} } ) {
			if   ( $compounds{$comps}{$inmodel}{compartment} ne "e" ) { $newid = "$comps\_$inmodel"; $newcompart = "$compounds{$comps}{$inmodel}{compartment}\_$inmodel"; }
			else                                                      { $newid = $comps;             $newcompart = "$compounds{$comps}{$inmodel}{compartment}"; }

			# $newid="$comps\_$inmodel";
			$joincompounds{$newid}{compartment} = $newcompart;
			$joincompounds{$newid}{formula}     = $compounds{$comps}{$inmodel}{formula};
			$joincompounds{$newid}{name}        = $compounds{$comps}{$inmodel}{name};
			$joincompounds{$newid}{charge}      = $compounds{$comps}{$inmodel}{charge};
			$joincompounds{$newid}{boundary}    = $compounds{$comps}{$inmodel}{boundary};
			$allcomparts{$newcompart}++;
		}

		#-- Faltan por ajustar las boundaries para extracelulares, tomara las del ultimo leido
	}
}

sub reacs {
	foreach my $reac ( sort keys %reactions ) {
		foreach my $inmodel ( sort keys %{ $reactions{$reac} } ) {
			$generic = 1;
			foreach my $prods ( sort keys %{ $compreactions{$reac}{$inmodel} } ) {
				foreach my $compinrec ( sort keys %{ $compreactions{$reac}{$inmodel}{$prods} } ) {
					if ( $compounds{$compinrec}{$inmodel}{compartment} ne "e" ) { $generic = 0; }
				}
			}
			if   ( !$generic ) { $newid = "$reac\_$inmodel"; }
			else               { $newid = "$reac"; }             #-- Si todos los componenetes son extracelulares, es una reaccion generica

			foreach my $prods ( sort keys %{ $compreactions{$reac}{$inmodel} } ) {    #-- Analiza los componentes de la reaccion
				foreach my $compinrec ( sort keys %{ $compreactions{$reac}{$inmodel}{$prods} } ) {
					if ( !$compounds{$compinrec}{$inmodel}{compartment} ) { die "ERROR: Cannot find chemical species $compinrec in model $inmodel\n"; }
					elsif ( $compounds{$compinrec}{$inmodel}{compartment} ne "e" ) {
						$newidmet = "$compinrec\_$inmodel";
						$generic  = 0;
					}
					else {
						$newidmet = $compinrec;
					}
					$joincompreactions{$newid}{$prods}{$newidmet} = $compreactions{$reac}{$inmodel}{$prods}{$compinrec};

					#  print " *$newid $prods\t$newidmet ($inmodel) $compounds{$compinrec}{$inmodel}{compartment}\n";
				}
			}

			#-- MODIFICACION PDSANCHEZ 10/12/2012
			#-- Identificar las reacciones de intercambio con el universo
			$generic = 0;
			foreach my $prods ( sort keys %{ $compreactions{$reac}{$inmodel} } ) {
				foreach my $compinrec ( sort keys %{ $compreactions{$reac}{$inmodel}{$prods} } ) {
					if ( $compounds{$compinrec}{$inmodel}{boundary} eq "true" ) { $generic = 1; }
				}
			}

			#print " *$generic > $newid -- $newname\n";
			#-- FIN MODIFICACION PDSANCHEZ 10/12/2012

			if ( !$generic ) {
				$newid   = "$reac\_$inmodel";
				$newname = "$reactions{$reac}{$inmodel}{reactionname}\_$inmodel";
				$lowerb  = $reactions{$reac}{$inmodel}{lowerbound};
				$upperb  = $reactions{$reac}{$inmodel}{upperbound};
			}
			else {
				$newid   = "$reac";
				$newname = $reactions{$reac}{$inmodel}{reactionname};

				#-- Sera una reaccion de intercambio, hay que mirar si esta en el medio minimo
				if ( !$minmediasw ) {    #-- No hay que ajustar medios
					$lowerb = $reactions{$reac}{$inmodel}{lowerbound};
					$upperb = $reactions{$reac}{$inmodel}{upperbound};
				}
				else {                   #-- Hay que ajustar le medio minimo
					if ( $minreac{$newid} ) {
						$lowerb = $reactions{$reac}{$inmodel}{lowerbound};
						$upperb = $reactions{$reac}{$inmodel}{upperbound};
					}                      #-- Esta en el medio minimo de alguna de las especies
					else {
						$lowerb = 0;

						#-- MODIFICACION PDSANCHEZ 10/12/2012
						#-- Quitar UB=0 (no permite salida de compuestos, se acumulan y el crecimiento es nulo)
						#$upperb=0;
						#-- Se deja el valor de UB de los modelos
						$upperb = $reactions{$reac}{$inmodel}{upperbound};

						#-- FIN MODIFICACION PDSANCHEZ 10/12/2012
					}
				}
				if ( $newobjective && ( $reac =~ /EX\_cpd11416\_c/ ) ) { $lowerb = 0; $upperb = 0; }    #-- Es una reaccion de intercambio de biomasa, la vamos a anular

			}

			# print "$reac\t$inmodel -> $newid\n";
			$joinreactions{$newid}{reactionname}  = $newname;
			$joinreactions{$newid}{reversibility} = $reactions{$reac}{$inmodel}{reversibility};
			$joinreactions{$newid}{lowerbound}    = $lowerb;
			$joinreactions{$newid}{upperbound}    = $upperb;

			#  $joinreactions{$newid}{upperbound}=$reactions{$reac}{$inmodel}{upperbound};
			if   ( !$newobjective ) { $joinreactions{$newid}{objectcoef} = $reactions{$reac}{$inmodel}{objectcoef}; }
			else                    { $joinreactions{$newid}{objectcoef} = "0.0"; }                                     #-- Vamos a aniadir una nueva funcion objetivo
			$joinreactions{$newid}{fluxvalue} = $reactions{$reac}{$inmodel}{fluxvalue};
		}
	}
}

sub objective {
	my $numm  = shift;
	my $bcomp = "TotBiomass_e";                                                                                     #-- Aniade reaccion de generacion de biomasa total
	my $breac = "Generation_Biomass_total";
	$joincompounds{$bcomp}{compartment}   = "e";
	$joincompounds{$bcomp}{formula}       = "";
	$joincompounds{$bcomp}{name}          = "Biomass_total";
	$joincompounds{$bcomp}{charge}        = 0;
	$joincompounds{$bcomp}{boundary}      = "false";
	$joinreactions{$breac}{reactionname}  = $breac;
	$joinreactions{$breac}{reversibility} = "false";
	$joinreactions{$breac}{lowerbound}    = 0;
	$joinreactions{$breac}{upperbound}    = 10000;
	if   ($newobjective) { $joinreactions{$breac}{objectcoef} = 1; }
	else                 { $joinreactions{$breac}{objectcoef} = 0; }
	$joinreactions{$breac}{fluxvalue} = 0;

	for ( my $nm = 1 ; $nm <= $numm ; $nm++ ) {

		# $biom="cpd11416\_c\_$nm";
		$biom = "$biomass{$nm}\_$nm";
		$joincompreactions{$breac}{react}{$biom} = 1;
	}
	$joincompreactions{$breac}{product}{$bcomp} = 1;

	my $bcompb = "TotBiomass_b";    #-- Aniade reaccion de intercambio de biomasa total (necesaria para que haya flujo)
	my $breac  = "EX_TotBiomass";
	$joincompounds{$bcompb}{compartment}         = "e";
	$joincompounds{$bcompb}{formula}             = "";
	$joincompounds{$bcompb}{name}                = "Biomass_total";
	$joincompounds{$bcompb}{charge}              = 0;
	$joincompounds{$bcompb}{boundary}            = "true";
	$joinreactions{$breac}{reactionname}         = $breac;
	$joinreactions{$breac}{reversibility}        = "false";
	$joinreactions{$breac}{lowerbound}           = 0;
	$joinreactions{$breac}{upperbound}           = 10000;
	$joinreactions{$breac}{objectcoef}           = 0;
	$joinreactions{$breac}{fluxvalue}            = 0;
	$joincompreactions{$breac}{react}{$bcomp}    = 1;
	$joincompreactions{$breac}{product}{$bcompb} = 1;
}

sub writeModel {

	#Encabezados
	print "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	print "<sbml xmlns=\"http://www.sbml.org/sbml/level2\" level=\"2\" version=\"1\" xmlns:html=\"http://www.w3.org/1999/xhtml\">\n";
	print "<model id=\"Combined\" name=\"$namemodel\">\n";

	#Unidades de medida

	print "<listOfUnitDefinitions>\n";
	print "<unitDefinition id=\"mmol_per_gDW_per_hr\">\n";
	print "<listOfUnits>\n";
	print "<unit kind=\"mole\" scale=\"-3\"/>\n";
	print "<unit kind=\"gram\" exponent=\"-1\"/>\n";
	print "<unit kind=\"second\" multiplier=\".00027777\" exponent=\"-1\"/>\n";
	print "</listOfUnits>\n";
	print "</unitDefinition>\n";
	print "</listOfUnitDefinitions>\n";

	#Primero formamos los compartimentos
	print "<listOfCompartments>\n";
	$compname = "";
	foreach my $compl ( sort keys %allcomparts ) {
		$outcmp = $outcompart{$compl};
		@idl = split( /\_/, $compl );
		if    ( $idl[0] eq "c" ) { $compname = "Cytosol"; }
		elsif ( $idl[0] eq "p" ) { $compname = "Periplasm"; }
		elsif ( $idl[0] eq "e" ) { $compname = "Extracellular"; }
		if ( $idl[1] ) { $compname .= "\_$idl[1]"; }
		print "<compartment id=\"$compl\" name=\"$compname\"";
		if ($outcmp) { print " outside=\"$outcmp\""; }
		print "/>\n";
	}
	print "</listOfCompartments>\n";

	#Lista de especies

	print "<listOfSpecies>\n";
	foreach my $tcomp ( sort keys %joincompounds ) {
		print "<species id=\"$tcomp\" name=\"$joincompounds{$tcomp}{name}\" compartment=\"$joincompounds{$tcomp}{compartment}\" charge=\"$joincompounds{$tcomp}{charge}\" boundaryCondition=\"$joincompounds{$tcomp}{boundary}\"";
		print "/>\n";
	}
	print "</listOfSpecies>\n";

	#Reacciones
	print "<listOfReactions>\n";
	foreach my $react ( sort keys %joinreactions ) {
		print "<reaction id=\"$react\" name=\"$joinreactions{$react}{reactionname}\" reversible=\"$joinreactions{$react}{reversibility}\">\n";
		if ( $joincompreactions{$react}{react} ) {
			print "<listOfReactants>\n";
			foreach my $reactant ( sort keys %{ $joincompreactions{$react}{react} } ) {
				print "<speciesReference species=\"$reactant\"  stoichiometry=\"$joincompreactions{$react}{react}{$reactant}\"/>\n";
			}
			print "</listOfReactants>\n";
		}
		if ( $joincompreactions{$react}{product} ) {
			print "<listOfProducts>\n";
			foreach my $product ( sort keys %{ $joincompreactions{$react}{product} } ) {
				print "<speciesReference species=\"$product\"  stoichiometry=\"$joincompreactions{$react}{product}{$product}\"/>\n";
			}
			print "</listOfProducts>\n";
		}
		print "<kineticLaw>\n";
		print "      <math xmlns=\"http://www.w3.org/1998/Math/MathML\">\n";
		print "          <apply>\n";
		print "          <ci> LOWER_BOUND </ci>\n";
		print "          <ci> UPPER_BOUND </ci>\n";
		print "          <ci> OBJECTIVE_COEFFICIENT </ci>\n";
		print "          <ci> FLUX_VALUE </ci>\n";
		print "          </apply>\n";
		print "      </math>\n";
		print "      <listOfParameters>\n";
		print "         <parameter id=\"LOWER_BOUND\" value=\"$joinreactions{$react}{lowerbound}\" units=\"mmol_per_gDW_per_hr\"/>\n";
		print "         <parameter id=\"UPPER_BOUND\" value=\"$joinreactions{$react}{upperbound}\" units=\"mmol_per_gDW_per_hr\"/>\n";
		print "         <parameter id=\"OBJECTIVE_COEFFICIENT\" value=\"$joinreactions{$react}{objectcoef}\"/>\n";
		print "         <parameter id=\"FLUX_VALUE\" value=\"$joinreactions{$react}{fluxvalue}\" units=\"mmol_per_gDW_per_hr\"/>\n";
		print "      </listOfParameters>\n";
		print "</kineticLaw>\n";
		print "</reaction>\n";
	}
	print "</listOfReactions>\n";
	print "</model>\n";
	print "</sbml>\n";

}

sub minmedia {
	my $minmedia = shift;
	my $numm     = shift;
	$minmedia =~ s/xml/minmedia_ul/;
	open( inm, $minmedia ) || die "Cannot read minimum media from $minmedia\n";
	while (<inm>) {
		chomp;
		( $ex, $namer ) = split( /\t/, $_ );
		$ex =~ s/\(e\)/\_e/g;
		$minreac{$ex}{$numm} = $namer;
	}
	close inm;
}
